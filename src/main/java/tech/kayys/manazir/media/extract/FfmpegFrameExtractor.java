package tech.kayys.manazir.media.extract;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import tech.kayys.manazir.media.time.MediaTimestamp;

/**
 * FFmpeg process adapter for single-frame extraction.
 *
 * <p>The source is never modified. Output is rendered to a sibling temporary file,
 * validated as an image, then moved into place without requesting replacement.
 * This adapter is not an OS-level sandbox for hostile media.</p>
 */
public final class FfmpegFrameExtractor implements FrameExtractor {
    public static final String BACKEND_ID = "ffmpeg-process";
    private final String executable;

    public FfmpegFrameExtractor() { this("ffmpeg"); }

    public FfmpegFrameExtractor(String executable) {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("executable must not be blank");
        this.executable = executable;
    }

    @Override public String backendId() { return BACKEND_ID; }

    @Override
    public FrameArtifact extract(FrameExtractionRequest request) throws FrameExtractionException {
        java.util.Objects.requireNonNull(request, "request");
        Path source = localSource(request.source());
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw failure(FrameExtractionException.Code.INVALID_SOURCE, "Source is not a readable regular file: " + source, null);
        }
        Path destination = request.destination();
        Path parent = destination.getParent();
        if (parent == null || !Files.isDirectory(parent) || !Files.isWritable(parent)) {
            throw failure(FrameExtractionException.Code.INVALID_DESTINATION, "Destination parent must be an existing writable directory", null);
        }
        if (Files.exists(destination)) {
            throw failure(FrameExtractionException.Code.INVALID_DESTINATION, "Destination already exists; refusing to overwrite: " + destination, null);
        }

        Path temporary;
        try {
            temporary = Files.createTempFile(parent, ".manazir-frame-", "." + request.format().extension());
        } catch (IOException exception) {
            throw failure(FrameExtractionException.Code.INVALID_DESTINATION, "Unable to create temporary output beside destination", exception);
        }

        try {
            run(request, source, temporary);
            long size = Files.size(temporary);
            if (size <= 0) throw failure(FrameExtractionException.Code.EXTRACTION_FAILED, "FFmpeg produced an empty frame", null);
            if (size > request.maxOutputBytes()) {
                throw failure(FrameExtractionException.Code.RESOURCE_LIMIT,
                        "Extracted frame exceeded maxOutputBytes=" + request.maxOutputBytes(), null);
            }
            BufferedImage image = ImageIO.read(temporary.toFile());
            if (image == null) throw failure(FrameExtractionException.Code.EXTRACTION_FAILED, "FFmpeg output is not a supported image", null);
            moveWithoutReplacement(temporary, destination);
            return new FrameArtifact(destination, request.format(), request.format().contentType(), size,
                    image.getWidth(), image.getHeight(), request.position(), request.streamIndex(), BACKEND_ID);
        } catch (IOException exception) {
            throw failure(FrameExtractionException.Code.EXTRACTION_FAILED, "Unable to validate or commit extracted frame", exception);
        } finally {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private void run(FrameExtractionRequest request, Path source, Path output) throws FrameExtractionException {
        List<String> args = new ArrayList<>(List.of(executable, "-nostdin", "-hide_banner", "-v", "error",
                "-i", source.toString(), "-ss", timestampSeconds(request.position()), "-map",
                "0:" + request.streamIndex(), "-frames:v", "1", "-an", "-sn", "-dn",
                "-c:v", request.format().ffmpegCodec(), "-fs", Long.toString(request.maxOutputBytes()),
                "-f", "image2", "-y", output.toString()));
        final Process process;
        try {
            process = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException exception) {
            throw failure(FrameExtractionException.Code.BACKEND_UNAVAILABLE,
                    "Unable to start FFmpeg executable '" + executable + "'", exception);
        }
        try {
            if (!process.waitFor(timeoutMillis(request.timeout()), TimeUnit.MILLISECONDS)) {
                terminateProcessTree(process);
                process.waitFor();
                throw failure(FrameExtractionException.Code.TIMEOUT, "FFmpeg exceeded timeout of " + request.timeout(), null);
            }
            if (process.exitValue() != 0) {
                throw failure(FrameExtractionException.Code.EXTRACTION_FAILED,
                        "FFmpeg exited with code " + process.exitValue(), null);
            }
        } catch (InterruptedException exception) {
            terminateProcessTree(process);
            Thread.currentThread().interrupt();
            throw failure(FrameExtractionException.Code.TIMEOUT, "FFmpeg wait was interrupted", exception);
        } finally {
            if (process.isAlive()) terminateProcessTree(process);
        }
    }

    private static String timestampSeconds(MediaTimestamp timestamp) {
        BigDecimal seconds = BigDecimal.valueOf(timestamp.ticks())
                .multiply(BigDecimal.valueOf(timestamp.timeBase().numerator()))
                .divide(BigDecimal.valueOf(timestamp.timeBase().denominator()), 12, RoundingMode.HALF_UP);
        return seconds.stripTrailingZeros().toPlainString();
    }

    private static Path localSource(URI uri) throws FrameExtractionException {
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            throw failure(FrameExtractionException.Code.INVALID_SOURCE, "This FFmpeg provider currently accepts file: URIs only", null);
        }
        try { return Path.of(uri).toAbsolutePath().normalize(); }
        catch (RuntimeException exception) {
            throw failure(FrameExtractionException.Code.INVALID_SOURCE, "Invalid local media URI: " + uri, exception);
        }
    }

    private static void moveWithoutReplacement(Path source, Path destination) throws IOException {
        // No REPLACE_EXISTING: a destination created by another writer wins the race.
        Files.move(source, destination);
    }

    private static void terminateProcessTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static long timeoutMillis(Duration timeout) {
        try { return Math.max(1, timeout.toMillis()); }
        catch (ArithmeticException exception) { return Long.MAX_VALUE; }
    }

    private static FrameExtractionException failure(FrameExtractionException.Code code, String message, Throwable cause) {
        return cause == null ? new FrameExtractionException(code, Optional.of(BACKEND_ID), message)
                : new FrameExtractionException(code, Optional.of(BACKEND_ID), message, cause);
    }
}
