package tech.kayys.manazir.media.extract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import tech.kayys.manazir.media.time.MediaTimestamp;

/**
 * FFmpeg process adapter that extracts a selected audio stream as PCM S16LE WAV.
 * This adapter is not an OS-level sandbox for hostile media.
 */
public final class FfmpegAudioExtractor implements AudioExtractor {
    public static final String BACKEND_ID = "ffmpeg-process";
    private static final long DIAGNOSTIC_LIMIT_BYTES = 64 * 1024;
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
                    .start();
        } catch (IOException exception) {
            throw failure(AudioExtractionException.Code.BACKEND_UNAVAILABLE,
                    "Unable to start FFmpeg executable '" + executable + "'", exception);
        }

        CompletableFuture<Capture> diagnostics = captureAsync(process.getErrorStream(), DIAGNOSTIC_LIMIT_BYTES, process);
        try {
            if (!process.waitFor(timeoutMillis(request.timeout()), TimeUnit.MILLISECONDS)) {
                terminate(process);
                process.waitFor();
                diagnostics.get();
                throw failure(AudioExtractionException.Code.TIMEOUT, "FFmpeg exceeded timeout of " + request.timeout(), null);
            }
            Capture captured = diagnostics.get();
            if (captured.truncated()) {
                throw failure(AudioExtractionException.Code.RESOURCE_LIMIT,
                        "FFmpeg diagnostic output exceeded " + DIAGNOSTIC_LIMIT_BYTES + " bytes; process was terminated", null);
            }
            if (process.exitValue() != 0) {
                String detail = new String(captured.bytes(), java.nio.charset.StandardCharsets.UTF_8).strip();
                String message = "FFmpeg exited with code " + process.exitValue() + " while extracting audio";
                if (!detail.isEmpty()) message += ": " + detail;
                throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, message, null);
            }
        } catch (InterruptedException exception) {
            terminate(process);
            Thread.currentThread().interrupt();
            throw failure(AudioExtractionException.Code.CANCELLED, "FFmpeg audio extraction was interrupted", exception);
        } catch (ExecutionException exception) {
            terminate(process);
            throw failure(AudioExtractionException.Code.EXTRACTION_FAILED,
                    "Unable to collect FFmpeg diagnostics", exception.getCause());
        } finally {
            if (process.isAlive()) terminate(process);
        }
    }

    private static CompletableFuture<Capture> captureAsync(InputStream input, long limit, Process process) {
        CompletableFuture<Capture> future = new CompletableFuture<>();
        Thread.ofVirtual().start(() -> {
            try (input) {
                future.complete(capture(input, limit, process));
            } catch (IOException exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private static Capture capture(InputStream input, long limit, Process process) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.min(limit, 8192));
        byte[] buffer = new byte[4096];
        long captured = 0;
        boolean truncated = false;
        int read;
        while ((read = input.read(buffer)) != -1) {
            int keep = (int) Math.min(read, Math.max(0, limit - captured));
            if (keep > 0) {
                output.write(buffer, 0, keep);
                captured += keep;
            }
            if (keep < read) {
                truncated = true;
                terminate(process);
                break;
            }
        }
        return new Capture(output.toByteArray(), truncated);
    }

    /**
     * Reads RIFF chunks rather than assuming the 44-byte PCM header layout.
     * FFmpeg and other WAV writers may place LIST/JUNK metadata chunks between
     * the fmt and data chunks, and chunks are padded to an even byte boundary.
     */
    private static WavInfo inspectWav(Path path) throws IOException, AudioExtractionException {
        long fileSize = Files.size(path);
        if (fileSize < 12) {
            throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV header is truncated", null);
        }

        try (var channel = java.nio.channels.FileChannel.open(path, java.nio.file.StandardOpenOption.READ)) {
            ByteBuffer riff = readAt(channel, 0, 12);
            byte[] signature = new byte[4];
            riff.get(signature);
            if (!new String(signature, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")) {
                throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "Output is not a RIFF WAV file", null);
            }
            long riffSize = Integer.toUnsignedLong(riff.getInt());
            riff.get(signature);
            if (!new String(signature, java.nio.charset.StandardCharsets.US_ASCII).equals("WAVE")
                    || riffSize < 4 || riffSize > fileSize - 8) {
                throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV RIFF container has an invalid size or signature", null);
            }

            long riffEnd = 8 + riffSize;
            long offset = 12;
            Integer format = null;
            Integer channels = null;
            Long sampleRate = null;
            Integer bits = null;
            Long dataBytes = null;

            while (offset + 8 <= riffEnd) {
                ByteBuffer chunkHeader = readAt(channel, offset, 8);
                chunkHeader.get(signature);
                long chunkSize = Integer.toUnsignedLong(chunkHeader.getInt());
                long payload = offset + 8;
                long chunkEnd = payload + chunkSize;
                if (chunkEnd < payload || chunkEnd > riffEnd) {
                    throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV contains a truncated RIFF chunk", null);
                }

                String chunkId = new String(signature, java.nio.charset.StandardCharsets.US_ASCII);
                if (chunkId.equals("fmt ")) {
                    if (chunkSize < 16) {
                        throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV fmt chunk is too short", null);
                    }
                    ByteBuffer fmt = readAt(channel, payload, 16);
                    format = Short.toUnsignedInt(fmt.getShort());
                    channels = Short.toUnsignedInt(fmt.getShort());
                    sampleRate = Integer.toUnsignedLong(fmt.getInt());
                    fmt.getInt(); // byte rate
                    fmt.getShort(); // block alignment
                    bits = Short.toUnsignedInt(fmt.getShort());
                } else if (chunkId.equals("data")) {
                    dataBytes = chunkSize;
                }

                offset = chunkEnd + (chunkSize & 1L);
            }

            if (format == null || channels == null || sampleRate == null || bits == null || dataBytes == null) {
                throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV is missing its fmt or data chunk", null);
            }
            if (format != 1 || bits != 16 || channels < 1 || sampleRate < 1) {
                throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV is not 16-bit integer PCM", null);
            }
            long bytesPerFrame = (long) channels * (bits / 8);
            if (dataBytes == 0 || dataBytes > fileSize || dataBytes % bytesPerFrame != 0) {
                throw failure(AudioExtractionException.Code.EXTRACTION_FAILED, "WAV data chunk has invalid length", null);
            }
            return new WavInfo(Math.toIntExact(sampleRate), channels, bits, dataBytes / bytesPerFrame);
        }
    }

    private static ByteBuffer readAt(java.nio.channels.FileChannel channel, long position, int length)
            throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer, position + buffer.position());
            if (read < 0) throw new IOException("Unexpected end of WAV file");
            if (read == 0) Thread.onSpinWait();
        }
        return buffer.flip();
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

    private record Capture(byte[] bytes, boolean truncated) {}\n\n    private record WavInfo(int sampleRate, int channels, int bitsPerSample, long sampleFrames) {}
}
