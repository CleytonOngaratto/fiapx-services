package br.com.fiap.fiapx.video.usecase.port.in;

import java.util.UUID;

public interface RequestUploadUseCase {

    UploadTicket execute(Command command);

    record Command(String ownerId, String filename, String contentType) {}

    record UploadTicket(UUID videoId, String objectKey, String uploadUrl, int expiresInSeconds) {}
}
