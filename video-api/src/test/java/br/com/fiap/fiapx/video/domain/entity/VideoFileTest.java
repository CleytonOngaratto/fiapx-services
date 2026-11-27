package br.com.fiap.fiapx.video.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class VideoFileTest {

    private static final UUID ID = UUID.fromString("a54499be-50c4-42ef-876f-b06c37b9cb9f");

    @Test
    void accepts_the_formats_of_the_base_project() {
        assertEquals("mp4", VideoFile.extensionOf("a.mp4"));
        assertEquals("mkv", VideoFile.extensionOf("a.MKV"));
        assertEquals("webm", VideoFile.extensionOf("muitos.pontos.no.nome.webm"));
    }

    @Test
    void rejects_unsupported_extension() {
        assertThrows(IllegalArgumentException.class, () -> VideoFile.extensionOf("a.pdf"));
    }

    @Test
    void rejects_filename_without_extension() {
        assertThrows(IllegalArgumentException.class, () -> VideoFile.extensionOf("semextensao"));
        assertThrows(IllegalArgumentException.class, () -> VideoFile.extensionOf("terminaemponto."));
    }

    @Test
    void rejects_blank_filename() {
        assertThrows(IllegalArgumentException.class, () -> VideoFile.extensionOf(" "));
        assertThrows(IllegalArgumentException.class, () -> VideoFile.extensionOf(null));
    }

    @Test
    void source_key_is_scoped_by_owner_and_video() {
        assertEquals(
                "videos/rm368677/a54499be-50c4-42ef-876f-b06c37b9cb9f/source.mp4",
                VideoFile.sourceKey("rm368677", ID, "palestra.mp4"));
    }

    @Test
    void source_key_rejects_blank_owner() {
        assertThrows(IllegalArgumentException.class, () -> VideoFile.sourceKey(" ", ID, "a.mp4"));
        assertThrows(IllegalArgumentException.class, () -> VideoFile.sourceKey(null, ID, "a.mp4"));
    }
}
