package br.com.fiap.fiapx.video.adapter.gateway;

import br.com.fiap.fiapx.video.usecase.port.out.ObjectStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ApplicationScoped
public class S3ObjectStorageGateway implements ObjectStoragePort {

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;
    private final int ttlSeconds;

    public S3ObjectStorageGateway(
            S3Client client,
            S3Presigner presigner,
            @ConfigProperty(name = "fiapx.s3.bucket-videos") String bucket,
            @ConfigProperty(name = "fiapx.s3.upload-ttl-seconds") int ttlSeconds) {
        this.client = client;
        this.presigner = presigner;
        this.bucket = bucket;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public PresignedUpload presignUpload(String objectKey, String contentType) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(ttlSeconds))
                .putObjectRequest(put)
                .build();

        return new PresignedUpload(presigner.presignPutObject(request).url().toString(), ttlSeconds);
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
