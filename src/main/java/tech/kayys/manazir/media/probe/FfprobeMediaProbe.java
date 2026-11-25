package tech.kayys.manazir.media.probe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.manazir.media.time.RationalTimeBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Isolated ffprobe backend. Uses argv (never a shell), enforces an in-memory
 * output ceiling while the child is running, and applies a wall-clock timeout.
 */
public final class FfprobeMediaProbe implements MediaProbe {
    private static final int MAX_OUTPUT_BYTES = 8 * 1024 * 1024;
    private static final int MAX_DIAGNOSTIC_CHARS = 2_048;
    private final String executable;
    private final ObjectMapper mapper;

    public FfprobeMediaProbe() { this("ffprobe", new ObjectMapper()); }

    public FfprobeMediaProbe(String executable, ObjectMapper mapper) {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("executable is blank");
        this.executable = executable;
        this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public MediaProbeResult probe(MediaProbeRequest request) throws MediaProbeException {
        if (!Files.isRegularFile(request.source()) || !Files.isReadable(request.source())) {
            throw new MediaProbeException(MediaProbeException.Reason.INVALID_INPUT,
                    "Source is not a readable regular file: " + request.source());
        }

        Process process = null;
        try {
            process = new ProcessBuilder(executable, "-v", "error", "-show_format", "-show_streams",
                    "-of", "json", request.source().toString()).redirectErrorStream(true).start();

            Process running = process;
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            AtomicBoolean outputExceeded = new AtomicBoolean();
            AtomicReference<IOException> readFailure = new AtomicReference<>();
            Thread reader = Thread.ofVirtual().name("manazir-ffprobe-output").start(() ->
                    captureBounded(running, captured, outputExceeded, readFailure));

            boolean completed = process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor();
                reader.join();
                throw new MediaProbeException(MediaProbeException.Reason.TIMEOUT,
                        "ffprobe exceeded timeout of " + request.timeout());
            }
            reader.join();

            if (outputExceeded.get()) {
                throw new MediaProbeException(MediaProbeException.Reason.OUTPUT_TOO_LARGE,
                        "ffprobe output exceeded " + MAX_OUTPUT_BYTES + " bytes");
            }
            if (readFailure.get() != null) {
                throw new MediaProbeException(MediaProbeException.Reason.PROCESS_FAILED,
                        "Unable to read ffprobe output", readFailure.get());
            }

            byte[] output = captured.toByteArray();
            if (process.exitValue() != 0) {
                String diagnostic = new String(output, StandardCharsets.UTF_8);
                if (diagnostic.length() > MAX_DIAGNOSTIC_CHARS)
                    diagnostic = diagnostic.substring(0, MAX_DIAGNOSTIC_CHARS) + "...";
                throw new MediaProbeException(MediaProbeException.Reason.PROCESS_FAILED,
                        "ffprobe exited with " + process.exitValue() + ": " + diagnostic);
            }
            return parse(request.source(), output);
        } catch (IOException e) {
            throw new MediaProbeException(MediaProbeException.Reason.BACKEND_UNAVAILABLE,
                    "Unable to execute ffprobe", e);
        } catch (InterruptedException e) {
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new MediaProbeException(MediaProbeException.Reason.PROCESS_FAILED, "ffprobe interrupted", e);
        }
    }

    private static void captureBounded(Process process, ByteArrayOutputStream output,
            AtomicBoolean exceeded, AtomicReference<IOException> failure) {
        byte[] buffer = new byte[8 * 1024];
        int total = 0;
        try (InputStream input = process.getInputStream()) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (total + read > MAX_OUTPUT_BYTES) {
                    exceeded.set(true);
                    process.destroyForcibly();
                    return;
                }
                output.write(buffer, 0, read);
                total += read;
            }
        } catch (IOException e) {
            if (process.isAlive()) failure.set(e);
        }
    }

    private MediaProbeResult parse(Path source, byte[] json) throws MediaProbeException {
        try {
            JsonNode root = mapper.readTree(json);
            if (root == null || !root.isObject())
                throw new IllegalArgumentException("root JSON value must be an object");
            JsonNode format = root.path("format");
            List<MediaStream> streams = new ArrayList<>();
            JsonNode streamNodes = root.path("streams");
            if (!streamNodes.isMissingNode() && !streamNodes.isArray())
                throw new IllegalArgumentException("streams must be an array");
            for (JsonNode node : streamNodes) streams.add(parseStream(node));
            return new MediaProbeResult(source, text(format, "format_name"), text(format, "format_long_name"),
                    decimal(format, "duration"), longValue(format, "size"), streams);
        } catch (IOException | RuntimeException e) {
            throw new MediaProbeException(MediaProbeException.Reason.INVALID_OUTPUT,
                    "ffprobe returned invalid or incomplete JSON", e);
        }
    }

    private MediaStream parseStream(JsonNode n) {
        String kind = text(n, "codec_type");
        MediaStreamType type = switch (kind == null ? "" : kind) {
            case "video" -> MediaStreamType.VIDEO;
            case "audio" -> MediaStreamType.AUDIO;
            case "subtitle" -> MediaStreamType.SUBTITLE;
            case "data" -> MediaStreamType.DATA;
            case "attachment" -> MediaStreamType.ATTACHMENT;
            default -> MediaStreamType.UNKNOWN;
        };
        return new MediaStream(integer(n, "index", 0), type, text(n, "codec_name"), rational(text(n, "time_base")),
                longValue(n, "start_pts"), longValue(n, "duration_ts"), nullableInt(n, "width"),
                nullableInt(n, "height"), text(n, "pix_fmt"), text(n, "sample_fmt"),
                nullableInt(n, "sample_rate"), nullableInt(n, "channels"), text(n, "channel_layout"),
                text(n, "avg_frame_rate"), text(n, "r_frame_rate"));
    }

    private static RationalTimeBase rational(String value) {
        if (value == null || value.equals("0/0")) return null;
        String[] parts = value.split("/", -1);
        if (parts.length != 2) return null;
        try {
            long numerator = Long.parseLong(parts[0]);
            long denominator = Long.parseLong(parts[1]);
            return numerator > 0 && denominator > 0 ? new RationalTimeBase(numerator, denominator) : null;
        } catch (NumberFormatException e) { return null; }
    }

    private static String text(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value == null || value.isNull() || value.asText().equals("N/A") ? null : value.asText();
    }
    private static Long longValue(JsonNode node, String key) {
        String value = text(node, key);
        try { return value == null ? null : Long.valueOf(value); }
        catch (NumberFormatException e) { return null; }
    }
    private static Integer nullableInt(JsonNode node, String key) {
        Long value = longValue(node, key);
        return value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? null : value.intValue();
    }
    private static int integer(JsonNode node, String key, int fallback) {
        Integer value = nullableInt(node, key);
        return value == null ? fallback : value;
    }
    private static BigDecimal decimal(JsonNode node, String key) {
        String value = text(node, key);
        try { return value == null ? null : new BigDecimal(value); }
        catch (NumberFormatException e) { return null; }
    }
}
