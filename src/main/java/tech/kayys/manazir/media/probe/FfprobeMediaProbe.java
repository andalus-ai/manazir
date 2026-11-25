package tech.kayys.manazir.media.probe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import tech.kayys.manazir.media.time.MediaTimestamp;
import tech.kayys.manazir.media.time.RationalTimeBase;

/**
 * Isolated FFprobe process provider. Arguments are passed directly (never through a shell),
 * stdout is bounded while the child runs, and no native library is loaded into the JVM.
 */
public final class FfprobeMediaProbe implements MediaProbe {
    public static final String BACKEND_ID = "ffprobe-process";
    private static final int STDERR_LIMIT = 64 * 1024;
    private static final RationalTimeBase CONTAINER_TIME_BASE = RationalTimeBase.of(1, 1_000_000);

    private final String executable;
    private final ObjectMapper mapper;

    public FfprobeMediaProbe() { this("ffprobe", new ObjectMapper()); }
    public FfprobeMediaProbe(String executable) { this(executable, new ObjectMapper()); }

    FfprobeMediaProbe(String executable, ObjectMapper mapper) {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("executable must not be blank");
        this.executable = executable;
        this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
    }

    @Override public String backendId() { return BACKEND_ID; }

    @Override
    public MediaProbeResult probe(MediaProbeRequest request) throws MediaProbeException {
        java.util.Objects.requireNonNull(request, "request");
        Path source = localSource(request.source());
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw failure(MediaProbeException.Code.INVALID_SOURCE, "Source is not a readable regular file: " + source, null);
        }
        if (request.maxProbeBytes() > Integer.MAX_VALUE - 8L) {
            throw failure(MediaProbeException.Code.RESOURCE_LIMIT, "maxProbeBytes exceeds the in-memory JSON parser limit", null);
        }

        final Process process;
        try {
            process = new ProcessBuilder(command(request, source)).start();
        } catch (IOException exception) {
            throw failure(MediaProbeException.Code.BACKEND_UNAVAILABLE,
                    "Unable to start FFprobe executable '" + executable + "'", exception);
        }

        CompletableFuture<Capture> stdout = captureAsync(process.getInputStream(), request.maxProbeBytes(), process, true);
        CompletableFuture<Capture> stderr = captureAsync(process.getErrorStream(), STDERR_LIMIT, process, false);
        try {
            if (!process.waitFor(timeoutMillis(request.timeout()), TimeUnit.MILLISECONDS)) {
                terminateProcessTree(process);
                process.waitFor();
                throw failure(MediaProbeException.Code.TIMEOUT, "FFprobe exceeded timeout of " + request.timeout(), null);
            }
            Capture output = stdout.get();
            Capture errors = stderr.get();
            if (output.truncated()) {
                throw failure(MediaProbeException.Code.RESOURCE_LIMIT,
                        "FFprobe JSON exceeded maxProbeBytes=" + request.maxProbeBytes(), null);
            }
            String diagnostic = new String(errors.bytes(), StandardCharsets.UTF_8).strip();
            if (process.exitValue() != 0) {
                throw failure(MediaProbeException.Code.PROBE_FAILED,
                        diagnostic.isEmpty() ? "FFprobe exited with code " + process.exitValue() : diagnostic, null);
            }
            return parse(request.source(), output.bytes());
        } catch (InterruptedException exception) {
            terminateProcessTree(process);
            Thread.currentThread().interrupt();
            throw failure(MediaProbeException.Code.CANCELLED, "FFprobe wait was interrupted", exception);
        } catch (ExecutionException exception) {
            terminateProcessTree(process);
            throw failure(MediaProbeException.Code.PROBE_FAILED, "Unable to collect FFprobe output", exception.getCause());
        } finally {
            if (process.isAlive()) terminateProcessTree(process);
        }
    }

    private List<String> command(MediaProbeRequest request, Path source) throws MediaProbeException {
        List<String> args = new ArrayList<>(List.of(executable, "-v", "error", "-show_format", "-show_streams", "-of", "json"));
        if (request.formatHint().isPresent()) {
            String hint = request.formatHint().get();
            if (!hint.matches("[A-Za-z0-9_,.-]+")) {
                throw failure(MediaProbeException.Code.INVALID_SOURCE, "formatHint contains unsupported characters", null);
            }
            args.add("-f");
            args.add(hint);
        }
        args.add("-i");
        args.add(source.toString());
        return List.copyOf(args);
    }

    private Path localSource(URI uri) throws MediaProbeException {
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            throw failure(MediaProbeException.Code.INVALID_SOURCE, "This FFprobe provider currently accepts file: URIs only", null);
        }
        try {
            return Path.of(uri).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw failure(MediaProbeException.Code.INVALID_SOURCE, "Invalid local media URI: " + uri, exception);
        }
    }

    private MediaProbeResult parse(URI source, byte[] json) throws MediaProbeException {
        try {
            JsonNode root = mapper.readTree(json);
            if (root == null || !root.isObject()) throw new IOException("output must be a JSON object");
            JsonNode format = root.path("format");
            String formatName = text(format, "format_name").orElse("unknown");
            Optional<MediaTimestamp> containerStart = decimalTimestamp(format, "start_time");
            Optional<MediaTimestamp> containerDuration = decimalTimestamp(format, "duration");
            JsonNode streamNodes = root.path("streams");
            if (!streamNodes.isArray()) throw new IOException("output does not contain a streams array");
            List<ProbeDiagnostic> diagnostics = new ArrayList<>();
            List<MediaStreamInfo> streams = new ArrayList<>();
            for (JsonNode node : streamNodes) streams.add(parseStream(node, diagnostics));
            MediaContainerInfo container = new MediaContainerInfo(formatName,
                    text(format, "format_long_name"), containerStart, containerDuration);
            return new MediaProbeResult(source, BACKEND_ID, container, streams, diagnostics);
        } catch (IOException | IllegalArgumentException exception) {
            throw failure(MediaProbeException.Code.PROBE_FAILED, "Unable to parse FFprobe JSON: " + exception.getMessage(), exception);
        }
    }

    private MediaStreamInfo parseStream(JsonNode node, List<ProbeDiagnostic> diagnostics) throws IOException {
        OptionalInt indexValue = integer(node, "index");
        if (indexValue.isEmpty()) throw new IOException("stream is missing a valid index");
        int index = indexValue.getAsInt();
        Optional<RationalTimeBase> timeBase = rational(node, "time_base");
        Optional<MediaTimestamp> start = streamTimestamp(longValue(node, "start_pts"), timeBase, index, "start_pts", diagnostics);
        Optional<MediaTimestamp> duration = streamTimestamp(longValue(node, "duration_ts"), timeBase, index, "duration_ts", diagnostics);
        OptionalInt width = positiveInt(node, "width");
        OptionalInt height = positiveInt(node, "height");
        if (width.isPresent() != height.isPresent()) {
            diagnostics.add(new ProbeDiagnostic(ProbeDiagnostic.Severity.WARNING, "INCOMPLETE_VIDEO_DIMENSIONS",
                    "Stream " + index + " has incomplete width/height metadata; both values omitted"));
            width = OptionalInt.empty();
            height = OptionalInt.empty();
        }
        return new MediaStreamInfo(index, text(node, "id"), streamType(text(node, "codec_type").orElse("")),
                text(node, "codec_name"), timeBase, start, duration, width, height,
                positiveInt(node, "sample_rate"), positiveInt(node, "channels"),
                frameRate(node, "avg_frame_rate"), frameRate(node, "r_frame_rate"));
    }

    private Optional<MediaTimestamp> streamTimestamp(Optional<Long> ticks, Optional<RationalTimeBase> timeBase,
            int index, String field, List<ProbeDiagnostic> diagnostics) {
        if (ticks.isEmpty()) return Optional.empty();
        if (timeBase.isEmpty()) {
            diagnostics.add(new ProbeDiagnostic(ProbeDiagnostic.Severity.WARNING, "MISSING_STREAM_TIME_BASE",
                    "Stream " + index + " has " + field + " but no time_base; timestamp omitted"));
            return Optional.empty();
        }
        return Optional.of(new MediaTimestamp(ticks.get(), timeBase.get()));
    }

    private Optional<MediaTimestamp> decimalTimestamp(JsonNode node, String field) throws IOException {
        Optional<String> value = text(node, field);
        if (value.isEmpty()) return Optional.empty();
        try {
            long ticks = new BigDecimal(value.get()).multiply(BigDecimal.valueOf(1_000_000))
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            return Optional.of(new MediaTimestamp(ticks, CONTAINER_TIME_BASE));
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IOException("invalid " + field + " value: " + value.get(), exception);
        }
    }

    private static Optional<RationalTimeBase> rational(JsonNode node, String field) {
        Optional<String> value = text(node, field);
        if (value.isEmpty()) return Optional.empty();
        String[] parts = value.get().split("/", -1);
        if (parts.length != 2) return Optional.empty();
        try { return Optional.of(RationalTimeBase.of(Long.parseLong(parts[0]), Long.parseLong(parts[1]))); }
        catch (IllegalArgumentException exception) { return Optional.empty(); }
    }

    private static Optional<FrameRate> frameRate(JsonNode node, String field) {
        Optional<String> value = text(node, field);
        if (value.isEmpty()) return Optional.empty();
        String[] parts = value.get().split("/", -1);
        if (parts.length != 2) return Optional.empty();
        try { return Optional.of(new FrameRate(Long.parseLong(parts[0]), Long.parseLong(parts[1]))); }
        catch (IllegalArgumentException exception) { return Optional.empty(); }
    }

    private static Optional<String> text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return Optional.empty();
        String result = value.asText();
        return result.isBlank() || "N/A".equalsIgnoreCase(result) ? Optional.empty() : Optional.of(result);
    }

    private static Optional<Long> longValue(JsonNode node, String field) {
        return text(node, field).flatMap(value -> {
            try { return Optional.of(Long.parseLong(value)); }
            catch (NumberFormatException exception) { return Optional.empty(); }
        });
    }

    private static OptionalInt integer(JsonNode node, String field) {
        Optional<Long> value = longValue(node, field);
        return value.isEmpty() || value.get() < Integer.MIN_VALUE || value.get() > Integer.MAX_VALUE
                ? OptionalInt.empty() : OptionalInt.of(value.get().intValue());
    }

    private static OptionalInt positiveInt(JsonNode node, String field) {
        OptionalInt value = integer(node, field);
        return value.isPresent() && value.getAsInt() > 0 ? value : OptionalInt.empty();
    }

    private static MediaStreamType streamType(String value) {
        return switch (value) {
            case "video" -> MediaStreamType.VIDEO;
            case "audio" -> MediaStreamType.AUDIO;
            case "subtitle" -> MediaStreamType.SUBTITLE;
            case "data" -> MediaStreamType.DATA;
            case "attachment" -> MediaStreamType.ATTACHMENT;
            default -> MediaStreamType.UNKNOWN;
        };
    }

    private static CompletableFuture<Capture> captureAsync(InputStream input, long limit, Process process, boolean killOnLimit) {
        CompletableFuture<Capture> future = new CompletableFuture<>();
        Thread.ofVirtual().start(() -> {
            try (input) { future.complete(capture(input, limit, process, killOnLimit)); }
            catch (IOException exception) { future.completeExceptionally(exception); }
        });
        return future;
    }

    private static Capture capture(InputStream input, long limit, Process process, boolean killOnLimit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.min(limit, 8192));
        byte[] buffer = new byte[8192];
        long captured = 0;
        boolean truncated = false;
        int read;
        while ((read = input.read(buffer)) != -1) {
            int keep = (int) Math.min(read, Math.max(0, limit - captured));
            if (keep > 0) { output.write(buffer, 0, keep); captured += keep; }
            if (keep < read) {
                truncated = true;
                if (killOnLimit) { terminateProcessTree(process); break; }
            }
        }
        return new Capture(output.toByteArray(), truncated);
    }


    private static void terminateProcessTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static long timeoutMillis(Duration timeout) {
        try { return Math.max(1, timeout.toMillis()); }
        catch (ArithmeticException exception) { return Long.MAX_VALUE; }
    }

    private static MediaProbeException failure(MediaProbeException.Code code, String message, Throwable cause) {
        return cause == null ? new MediaProbeException(code, Optional.of(BACKEND_ID), message)
                : new MediaProbeException(code, Optional.of(BACKEND_ID), message, cause);
    }

    private record Capture(byte[] bytes, boolean truncated) {}
}