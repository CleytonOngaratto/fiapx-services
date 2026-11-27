package br.com.fiap.fiapx.video.usecase.port.out;

public interface ObjectStoragePort {

    PresignedUpload presignUpload(String objectKey, String contentType);

    boolean exists(String objectKey);

    record PresignedUpload(String url, int expiresInSeconds) {}
}
