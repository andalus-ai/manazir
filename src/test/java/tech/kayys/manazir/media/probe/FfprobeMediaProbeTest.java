package tech.kayys.manazir.media.probe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.LINUX)
class FfprobeMediaProbeTest {
    @TempDir Path temp;

    @Test void parsesTypedMetadataAndPreservesPtsTimeBase() throws Exception {
        Path source = Files.createFile(temp.resolve("input file.mp4"));
        String json = """
                {"format":{"format_name":"mov,mp4","duration":"12.5","size":"2048"},
                 "streams":[{"index":0,"codec_type":"video","codec_name":"h264","time_base":"1/90000",
                   "start_pts":"9000","duration_ts":"1125000","width":1920,"height":1080,
                   "pix_fmt":"yuv420p","avg_frame_rate":"25/1","r_frame_rate":"25/1"},
                  {"index":1,"codec_type":"audio","codec_name":"aac","time_base":"1/48000",
                   "start_pts":"0","duration_ts":"600000","sample_rate":"48000","channels":2,
                   "channel_layout":"stereo","sample_fmt":"fltp"}]}
                """;
        Path executable = script("printf '%s' '" + json.replace("'", "'\\''").replace("\n", "") + "'");
        var result = new FfprobeMediaProbe(executable.toString(), new ObjectMapper())
                .probe(MediaProbeRequest.of(source));

        assertEquals("mov,mp4", result.formatName());
        assertEquals(2, result.streams().size());
        var video = result.streams(MediaStreamType.VIDEO).getFirst();
        assertEquals(1920, video.width());
        assertEquals(9_000L, video.startTimestamp().orElseThrow().ticks());
        assertEquals(90_000, video.startTimestamp().orElseThrow().timeBase().denominator());
        assertEquals("stereo", result.streams(MediaStreamType.AUDIO).getFirst().channelLayout());
    }

    @Test void terminatesAProcessThatExceedsTheOutputCeiling() throws Exception {
        Path source = Files.createFile(temp.resolve("large-input.mp4"));
        Path executable = script("head -c 9000000 /dev/zero");
        MediaProbeException error = assertThrows(MediaProbeException.class, () ->
                new FfprobeMediaProbe(executable.toString(), new ObjectMapper())
                        .probe(MediaProbeRequest.of(source)));
        assertEquals(MediaProbeException.Reason.OUTPUT_TOO_LARGE, error.reason());
    }

    private Path script(String command) throws Exception {
        Path executable = temp.resolve("fake ffprobe");
        Files.writeString(executable, "#!/bin/sh\n" + command + "\n");
        Files.setPosixFilePermissions(executable, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        return executable;
    }
}
