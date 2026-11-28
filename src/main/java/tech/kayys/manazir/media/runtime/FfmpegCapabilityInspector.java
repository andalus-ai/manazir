package tech.kayys.manazir.media.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Explicit, bounded inspection of FFmpeg's advertised operation tables.
 * This runs subprocesses only when inspect is called; it never loads native libraries.
 */
public final class FfmpegCapabilityInspector {
    private static final int OUTPUT_LIMIT = 2 * 1024 * 1024;
    private static final Pattern CODEC = Pattern.compile("^\\s*([A-Z.]{6})\\s+([A-Za-z0-9_.-]+)\\s+.*$");
    private static final Pattern FILTER = Pattern.compile("^\\s*([TSC.]{3})\\s+([A-Za-z0-9_.-]+)\\s+.*$");
    private static final Pattern FORMAT = Pattern.compile("^\\s*([D.E]{1,2})\\s+([A-Za-z0-9_,.-]+)\\s+.*$");

    private final String executable;
    private final Duration timeout;

    public FfmpegCapabilityInspector() {
        this("ffmpeg", Duration.ofSeconds(5));
    }

    public FfmpegCapabilityInspector(String executable, Duration timeout) {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("executable must not be blank");
        if (timeout == null || timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        this.executable = executable;
        this.timeout = timeout;
    }

    public FfmpegCapabilityReport inspect() throws FfmpegInspectionException {
        String versionOutput = run("-version");
        String version = versionOutput.lines().filter(line -> line.startsWith("ffmpeg version"))
                .findFirst().orElse("");
        String decoders = run("-hide_banner", "-decoders");
        String encoders = run("-hide_banner", "-encoders");
        String filters = run("-hide_banner", "-filters");
        String muxers = run("-hide_banner", "-muxers");
        String demuxers = run("-hide_banner", "-demuxers");

        Set<String> videoDecoders = codecs(decoders, 'V');
        Set<String> audioDecoders = codecs(decoders, 'A');
        Set<String> videoEncoders = codecs(encoders, 'V');
        Set<String> audioEncoders = codecs(encoders, 'A');
        Set<String> filterNames = entries(filters, FILTER, 2);
        Set<String> muxerNames = formats(muxers, 'E');
        Set<String> demuxerNames = formats(demuxers, 'D');
        List<String> diagnostics = new ArrayList<>();
        if (videoDecoders.isEmpty()) diagnostics.add("FFmpeg reports no video decoders");
        if (audioDecoders.isEmpty()) diagnostics.add("FFmpeg reports no audio decoders");
        if (filterNames.isEmpty()) diagnostics.add("FFmpeg reports no filters");
        return new FfmpegCapabilityReport(executable, version, videoDecoders, audioDecoders,
                videoEncoders, audioEncoders, filterNames, muxerNames, demuxerNames, diagnostics);
    }

    private String run(String... arguments) throws FfmpegInspectionException {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.addAll(List.of(arguments));
        final Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException exception) {
            throw new FfmpegInspectionException(FfmpegInspectionException.Code.EXECUTABLE_UNAVAILABLE,
                    "Unable to start FFmpeg executable '" + executable + "'", exception);
        }
        CompletableFuture<Capture> capture = CompletableFuture.supplyAsync(() -> {
            try (InputStream input = process.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > OUTPUT_LIMIT) {
                        terminate(process);
                        throw new IOException("FFmpeg capability output exceeded " + OUTPUT_LIMIT + " bytes");
                    }
                    output.write(buffer, 0, count);
                }
                return new Capture(output.toByteArray());
            } catch (IOException exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        });
        try {
            if (!process.waitFor(timeoutMillis(), TimeUnit.MILLISECONDS)) {
                terminate(process);
                throw new FfmpegInspectionException(FfmpegInspectionException.Code.TIMEOUT,
                        "FFmpeg command exceeded timeout " + timeout + ": " + arguments[0]);
            }
            Capture result = capture.get();
            if (process.exitValue() != 0) {
                throw new FfmpegInspectionException(FfmpegInspectionException.Code.COMMAND_FAILED,
                        "FFmpeg command " + arguments[0] + " exited with code " + process.exitValue());
            }
            return new String(result.bytes(), StandardCharsets.UTF_8);
        } catch (InterruptedException exception) {
            terminate(process);
            Thread.currentThread().interrupt();
            throw new FfmpegInspectionException(FfmpegInspectionException.Code.INTERRUPTED,
                    "FFmpeg capability inspection was interrupted", exception);
        } catch (ExecutionException exception) {
            terminate(process);
            throw new FfmpegInspectionException(FfmpegInspectionException.Code.OUTPUT_LIMIT,
                    "Unable to capture bounded FFmpeg capability output", exception.getCause());
        } finally {
            if (process.isAlive()) terminate(process);
        }
    }

    private static Set<String> codecs(String output, char mediaType) {
        Set<String> result = new LinkedHashSet<>();
        for (String line : output.lines().toList()) {
            Matcher matcher = CODEC.matcher(line);
            if (matcher.matches()) {
                String flags = matcher.group(1);
                if (flags.charAt(0) == mediaType) result.add(matcher.group(2));
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> formats(String output, char operation) {
        Set<String> result = new LinkedHashSet<>();
        for (String line : output.lines().toList()) {
            Matcher matcher = FORMAT.matcher(line);
            if (matcher.matches() && matcher.group(1).indexOf(operation) >= 0) {
                for (String name : matcher.group(2).split(",")) result.add(name);
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> entries(String output, Pattern pattern, int nameGroup) {
        Set<String> result = new LinkedHashSet<>();
        for (String line : output.lines().toList()) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) result.add(matcher.group(nameGroup));
        }
        return Set.copyOf(result);
    }

    private long timeoutMillis() {
        try {
            return Math.max(1, timeout.toMillis());
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private static void terminate(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private record Capture(byte[] bytes) { }
}
