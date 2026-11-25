package tech.kayys.manazir.media.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExecutableAvailabilityCheckTest {
    @TempDir Path tempDir;

    @Test
    void reportsRunnableExecutableWithoutClaimingMediaCapabilities() throws Exception {
        Path executable = script("exit 0");
        var result = ExecutableAvailabilityCheck.inspect(executable.toString(), Duration.ofSeconds(1));
        assertEquals(ProviderAvailability.AVAILABLE, result.availability());
        assertTrue(result.diagnostic().contains("-version"));
    }

    @Test
    void reportsNonZeroVersionCommandAsUnavailable() throws Exception {
        Path executable = script("exit 4");
        var result = ExecutableAvailabilityCheck.inspect(executable.toString(), Duration.ofSeconds(1));
        assertEquals(ProviderAvailability.UNAVAILABLE, result.availability());
    }

    @Test
    void reportsMissingExecutableAsUnavailable() {
        var result = ExecutableAvailabilityCheck.inspect(
                tempDir.resolve("missing").toString(), Duration.ofSeconds(1));
        assertEquals(ProviderAvailability.UNAVAILABLE, result.availability());
    }

    @Test
    void enforcesVersionCheckTimeout() throws Exception {
        Path executable = script("sleep 2");
        var result = ExecutableAvailabilityCheck.inspect(executable.toString(), Duration.ofMillis(50));
        assertEquals(ProviderAvailability.UNAVAILABLE, result.availability());
        assertTrue(result.diagnostic().contains("timeout"));
    }

    private Path script(String body) throws IOException {
        Path path = tempDir.resolve("fake-tool-" + UUID.randomUUID() + ".sh");
        Files.writeString(path, "#!/bin/sh\n" + body + "\n");
        if (!path.toFile().setExecutable(true)) throw new IOException("cannot make test executable");
        return path;
    }
}
