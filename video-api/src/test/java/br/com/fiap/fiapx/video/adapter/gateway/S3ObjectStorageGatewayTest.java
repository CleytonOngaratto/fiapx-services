package br.com.fiap.fiapx.video.adapter.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.fiap.fiapx.video.usecase.port.out.ObjectStoragePort;
import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3ObjectStorageGatewayTest {

    private static final String BUCKET = "fiapx-videos";

    private static S3MockContainer container;
    private static S3ObjectStorageGateway gateway;

    @BeforeAll
    static void startStorage() {
        container = new S3MockContainer("latest");
        container.start();

        URI endpoint = URI.create(container.getHttpEndpoint());
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
        var pathStyle = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        S3Client client = S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(endpoint)
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();

        S3Presigner presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(endpoint)
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();

        client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        gateway = new S3ObjectStorageGateway(client, presigner, BUCKET, 900);
    }

    @AfterAll
    static void stopStorage() {
        container.stop();
    }

    @Test
    void the_presigned_url_actually_accepts_the_put() throws Exception {
        String key = "videos/rm368677/11111111-2222-3333-4444-555555555555/source.mp4";

        ObjectStoragePort.PresignedUpload upload = gateway.presignUpload(key, "video/mp4");
        assertEquals(900, upload.expiresInSeconds());
        assertFalse(gateway.exists(key));

        HttpResponse<Void> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(upload.url()))
                        .header("Content-Type", "video/mp4")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(new byte[4096]))
                        .build(),
                HttpResponse.BodyHandlers.discarding());

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("ETag").isPresent());
        assertTrue(gateway.exists(key));
    }

    @Test
    void exists_is_false_for_an_absent_key() {
        assertFalse(gateway.exists("videos/rm1/nao-existe/source.mp4"));
    }
}
