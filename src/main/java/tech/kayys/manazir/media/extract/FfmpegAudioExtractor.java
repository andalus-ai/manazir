package tech.kayys.manazir.media.extract;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import tech.kayys.manazir.media.time.MediaTimestamp;

/**
 * FFmpeg process adapter that extracts a selected audio stream as PCM S16LE WAV.
 * This adapter is not an OS-level sandbox for hostile media.
 */
public final class FfmpegAudioExtractor implements AudioExtractor {
    public static final String BACKEND_ID = "ffmpeg-process";
    private final String executable;

    public FfmpegAudioExtractor() { this("ffmpeg"); }

    public FfmpegAudioExtractor(String executable) {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("executable must not be blank");
        this.executable = executable;
    }

    @Override public String backendId() { return BACKEND_ID; }

    @Override
    public AudioArtifact extract(AudioExtractionRequest request) throws AudioExtractionException {
        java.util.Objects.requireNonNull(request, "request");
        Path source = localSource(request.source());
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw failure(AudioExtractionException.Code.INVALID_SOURCE, "Source is not a readable regular file: " + source, null);
        }
        Path destination = request.output();
        Path parent = destination.getParent();
        if (parent == null || !Files.isDirectory(parent) || !Files.isWritable(parent)) {
            throw failure(AudioExtractionException.Code.INVALID_DESTINATION, "Destination parent must be an existing writable directory", null);
        }
        if (Files.exists(destination)) {
            throw failure(AudioExtractionException.Code.INVALID_DESTINATION, "Destination already exists; refusing to overwrite: " + destination, null);
        }

        Path temporary;
        try {
            temporary = Files.createTempFile(parent, ".manazir-audio-", ".wav");
        } catch (IOException exception) {
            throw failure(AudioExtractionException.Code.INVALID_DESTINATION, "Unable to create temporary output beside destination", exception);
        }

        try {
            run(request, source, temporary);
            long size = Files.size(temporary);
            if (size <= 44) throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "FFmpeg produced an empty WAV segment", null);
            if (size > request.maxOutputBytes()) {
                throw failure(AudioExtractionException.Code.RESOURCE_LIMIT,
                        "Extracted audio exceeded maxOutputBytes=" + request.maxOutputBytes(), null);
            }
            WavInfo wav = inspectWav(temporary);
            if (wav.sampleRate() != request.sampleRate() || wav.channels() != request.channels()) {
                throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV output does not match requested sample rate/channel count", null);
            }
            Files.move(temporary, destination);
            return new AudioArtifact(destination, "audio/wav", size, wav.sampleRate(), wav.channels(),
                    wav.bitsPerSample(), wav.sampleFrames(), request.start(), request.end(),
                    request.audioStreamIndex(), BACKEND_ID);
        } catch (IOException exception) {
            throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "Unable to validate or commit extracted WAV segment", exception);
        } finally {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private void run(AudioExtractionRequest request, Path source, Path output) throws AudioExtractionException {
        String start = seconds(request.start());
        String duration = secondsBetween(request.start(), request.end());
        List<String> args = new ArrayList<>(List.of(executable, "-nostdin", "-hide_banner", "-loglevel", "error",
                "-i", source.toString(), "-ss", start, "-t", duration,
                "-map", "0:" + request.audioStreamIndex(), "-vn", "-sn", "-dn",
                "-ac", Integer.toString(request.channels()), "-ar", Integer.toString(request.sampleRate()),
                "-c:a", "pcm_s16le", "-f", "wav", "-fs", Long.toString(request.maxOutputBytes()),
                "-y", output.toString()));
        final Process process;
        try {
            process = new ProcessBuilder(args)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException exception) {
            throw failure(AudioExtractionException.Code.BACKEND_UNAVAILABLE,
                    "Unable to start FFmpeg executable '" + executable + "'", exception);
        }
        try {
            if (!process.waitFor(timeoutMillis(request.timeout()), TimeUnit.MILLISECONDS)) {
                terminate(process);
                process.waitFor();
                throw failure(AudioExtractionException.Code.TIMEOUT, "FFmpeg exceeded timeout of " + request.timeout(), null);
            }
            if (process.exitValue() != 0) {
                throw failure(AudioExtractionException.Code.EXTRACTION_FAILED,
                        "FFmpeg exited with code " + process.exitValue() + " while extracting audio", null);
            }
        } catch (InterruptedException exception) {
            terminate(process);
            Thread.currentThread().interrupt();
            throw failure(AudioExtractionException.Code.CANCELLED, "FFmpeg audio extraction was interrupted", exception);
        } finally {
            if (process.isAlive()) terminate(process);
        }
    }

    private static WavInfo inspectWav(Path path) throws IOException, AudioExtractionException {
        long size = Files.size(path);
        if (size < 44) throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV header is truncated", null);
        byte[] header = new byte[44];
        try (var input = Files.newInputStream(path)) {
            if (input.readNBytes(header, 0, header.length) != header.length) {
                throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV header is truncated", null);
            }
        }
        ByteBuffer b = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        if (!ascii(header, 0, "RIFF") || !ascii(header, 8, "WAVE")
                || !ascii(header, 12, "fmt ") || !ascii(header, 36, "data")) {
            throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "Output is not a canonical PCM WAV file", null);
        }
        int format = Short.toUnsignedInt(b.getShort(20));
        int channels = Short.toUnsignedInt(b.getShort(22));
        long sampleRate = Integer.toUnsignedLong(b.getInt(24));
        int bits = Short.toUnsignedInt(b.getShort(34));
        long dataBytes = Integer.toUnsignedLong(b.getInt(40));
        if (format != 1 || bits != 16 || channels < 1 || sampleRate < 1) {
            throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV is not 16-bit integer PCM", null);
        }
        long bytesPerFrame = (long) channels * (bits / 8);
        if (dataBytes == 0 || dataBytes > size - 44 || dataBytes % bytesPerFrame != 0) {
            throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV data chunk has invalid length", null);
        }
        return new WavInfo(Math.toIntExact(sampleRate), channels, bits, dataBytes / bytesPerFrame);
    }

    private static boolean ascii(byte[] bytes, int offset, String value) {
        for (int i = 0; i < value.length(); i++) if (bytes[offset + i] != (byte) value.charAt(i)) return false;
        return true;
    }

    private static String seconds(MediaTimestamp timestamp) {
        return BigDecimal.valueOf(timestamp.ticks()).multiply(BigDecimal.valueOf(timestamp.timeBase().numerator()))
                .divide(BigDecimal.valueOf(timestamp.timeBase().denominator()), 12, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    private static String secondsBetween(MediaTimestamp start, MediaTimestamp end) {
        BigDecimal startSeconds = new BigDecimal(start.ticks())
                .multiply(BigDecimal.valueOf(start.timeBase().numerator()))
                .divide(BigDecimal.valueOf(start.timeBase().denominator()), 24, RoundingMode.HALF_UP);
        BigDecimal endSeconds = new BigDecimal(end.ticks())
                .multiply(BigDecimal.valueOf(end.timeBase().numerator()))
                .divide(BigDecimal.valueOf(end.timeBase().denominator()), 24, RoundingMode.HALF_UP);
        return endSeconds.subtract(startSeconds).setScale(12, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    private static Path localSource(URI uri) throws AudioExtractionException {
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            throw failure(AudioExtractionException.Code.INVALID_SOURCE, "This FFmpeg provider currently accepts file: URIs only", null);
        }
        try { return Path.of(uri).toAbsolutePath().normalize(); }
        catch (RuntimeException exception) {
            throw failure(AudioExtractionException.Code.INVALID_SOURCE, "Invalid local media URI: " + uri, exception);
        }
    }

    private static long timeoutMillis(Duration timeout) {
        try { return Math.max(1, timeout.toMillis()); }
        catch (ArithmeticException exception) { return Long.MAX_VALUE; }
    }

    private static void terminate(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static AudioExtractionException failure(AudioExtractionException.Code code, String message, Throwable cause) {
        return cause == null ? new AudioExtractionException(code, Optional.of(BACKEND_ID), message)
                : new AudioExtractionException(code, Optional.of(BACKEND_ID), message, cause);
    }

    private record WavInfo(int sampleRate, int channels, int bitsPerSample, long sampleFrames) {}
}
