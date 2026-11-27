package br.com.fiap.fiapx.video.usecase.interactor;

import br.com.fiap.fiapx.video.domain.entity.VideoFile;
import br.com.fiap.fiapx.video.usecase.port.in.RequestUploadUseCase;
import br.com.fiap.fiapx.video.usecase.port.out.ObjectStoragePort;
import java.util.UUID;

public class RequestUploadInteractor implements RequestUploadUseCase {

    private final ObjectStoragePort storage;

    public RequestUploadInteractor(ObjectStoragePort storage) {
        this.storage = storage;
    }

    @Override
    public UploadTicket execute(Command command) {
        UUID videoId = UUID.randomUUID();
        String objectKey = VideoFile.sourceKey(command.ownerId(), videoId, command.filename());

        ObjectStoragePort.PresignedUpload presigned =
                storage.presignUpload(objectKey, command.contentType());

        return new UploadTicket(videoId, objectKey, presigned.url(), presigned.expiresInSeconds());
    }
}
