package br.com.fiap.fiapx.video.domain.entity;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class VideoFile {

    private static final Set<String> SUPPORTED =
            Set.of("mp4", "avi", "mov", "mkv", "wmv", "flv", "webm");

    private VideoFile() {}

    public static String extensionOf(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename obrigatorio");
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new IllegalArgumentException("filename sem extensao: " + filename);
        }
        String extension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED.contains(extension)) {
            throw new IllegalArgumentException("formato nao suportado: " + extension);
        }
        return extension;
    }

    public static String sourceKey(String ownerId, UUID videoId, String filename) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId obrigatorio");
        }
        return "videos/%s/%s/source.%s".formatted(ownerId, videoId, extensionOf(filename));
    }
}
