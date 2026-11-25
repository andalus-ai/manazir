package tech.kayys.manazir.media.probe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.manazir.media.time.RationalTimeBase;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Isolated ffprobe backend. Uses argv (never a shell) and bounds JSON before parsing. */
public final class FfprobeMediaProbe implements MediaProbe {
    private static final long MAX_OUTPUT_BYTES = 8L * 1024 * 1024;
    private final String executable;
    private final ObjectMapper mapper;
    public FfprobeMediaProbe() { this("ffprobe", new ObjectMapper()); }
    public FfprobeMediaProbe(String executable, ObjectMapper mapper) {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("executable is blank");
        this.executable = executable;
        this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
    }

    @Override public MediaProbeResult probe(MediaProbeRequest request) throws MediaProbeException {
        if (!Files.isRegularFile(request.source()) || !Files.isReadable(request.source()))
            throw new MediaProbeException(MediaProbeException.Reason.INVALID_INPUT, "Source is not a readable regular file: " + request.source());
        Path output = null, errors = null;
        Process process = null;
        try {
            output = Files.createTempFile("manazir-ffprobe-", ".json");
            errors = Files.createTempFile("manazir-ffprobe-", ".log");
            process = new ProcessBuilder(executable, "-v", "error", "-show_format", "-show_streams",
                    "-of", "json", request.source().toString()).redirectOutput(output.toFile()).redirectError(errors.toFile()).start();
            if (!process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly(); process.waitFor();
                throw new MediaProbeException(MediaProbeException.Reason.TIMEOUT, "ffprobe exceeded timeout of " + request.timeout());
            }
            if (Files.size(output) > MAX_OUTPUT_BYTES)
                throw new MediaProbeException(MediaProbeException.Reason.OUTPUT_TOO_LARGE, "ffprobe JSON exceeded " + MAX_OUTPUT_BYTES + " bytes");
            if (process.exitValue() != 0)
                throw new MediaProbeException(MediaProbeException.Reason.PROCESS_FAILED,
                        "ffprobe exited with " + process.exitValue() + ": " + Files.readString(errors));
            return parse(request.source(), Files.readAllBytes(output));
        } catch (IOException e) {
            throw new MediaProbeException(MediaProbeException.Reason.BACKEND_UNAVAILABLE, "Unable to execute or read ffprobe output", e);
        } catch (InterruptedException e) {
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new MediaProbeException(MediaProbeException.Reason.PROCESS_FAILED, "ffprobe interrupted", e);
        } finally { deleteQuietly(output); deleteQuietly(errors); }
    }

    private MediaProbeResult parse(Path source, byte[] json) throws MediaProbeException {
        try {
            JsonNode root = mapper.readTree(json), format = root.path("format");
            List<MediaStream> streams = new ArrayList<>();
            for (JsonNode node : root.path("streams")) streams.add(parseStream(node));
            return new MediaProbeResult(source, text(format,"format_name"), text(format,"format_long_name"),
                    decimal(format,"duration"), longValue(format,"size"), streams);
        } catch (IOException | RuntimeException e) {
            throw new MediaProbeException(MediaProbeException.Reason.INVALID_OUTPUT, "ffprobe returned invalid or incomplete JSON", e);
        }
    }
    private MediaStream parseStream(JsonNode n) {
        String kind = text(n,"codec_type");
        MediaStreamType type = switch (kind == null ? "" : kind) {
            case "video" -> MediaStreamType.VIDEO; case "audio" -> MediaStreamType.AUDIO;
            case "subtitle" -> MediaStreamType.SUBTITLE; case "data" -> MediaStreamType.DATA;
            case "attachment" -> MediaStreamType.ATTACHMENT; default -> MediaStreamType.UNKNOWN;
        };
        return new MediaStream(integer(n,"index",0), type, text(n,"codec_name"), rational(text(n,"time_base")),
                longValue(n,"start_pts"), longValue(n,"duration_ts"), nullableInt(n,"width"), nullableInt(n,"height"),
                text(n,"pix_fmt"), text(n,"sample_fmt"), nullableInt(n,"sample_rate"), nullableInt(n,"channels"),
                text(n,"channel_layout"), text(n,"avg_frame_rate"), text(n,"r_frame_rate"));
    }
    private static RationalTimeBase rational(String value) {
        if (value == null || value.equals("0/0")) return null;
        String[] p = value.split("/",-1);
        if (p.length != 2) return null;
        try { long a=Long.parseLong(p[0]), b=Long.parseLong(p[1]); return a>0 && b>0 ? new RationalTimeBase(a,b) : null; }
        catch (NumberFormatException e) { return null; }
    }
    private static String text(JsonNode n,String k) {
        JsonNode v=n.get(k); return v==null || v.isNull() || v.asText().equals("N/A") ? null : v.asText();
    }
    private static Long longValue(JsonNode n,String k) {
        String v=text(n,k); try { return v==null ? null : Long.valueOf(v); } catch(NumberFormatException e) { return null; }
    }
    private static Integer nullableInt(JsonNode n,String k) {
        Long v=longValue(n,k); return v==null || v<Integer.MIN_VALUE || v>Integer.MAX_VALUE ? null : v.intValue();
    }
    private static int integer(JsonNode n,String k,int fallback) { Integer v=nullableInt(n,k); return v==null?fallback:v; }
    private static BigDecimal decimal(JsonNode n,String k) {
        String v=text(n,k); try { return v==null?null:new BigDecimal(v); } catch(NumberFormatException e) { return null; }
    }
    private static void deleteQuietly(Path p) { if(p!=null) try { Files.deleteIfExists(p); } catch(IOException ignored) { } }
}
