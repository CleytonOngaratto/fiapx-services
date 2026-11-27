package br.com.fiap.fiapx.video.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import java.net.URI;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ApplicationScoped
public class S3PresignerProducer {

    // Endpoint do PRESIGNER, separado do `quarkus.s3.endpoint-override` de propósito: a URL
    // assinada é consumida pelo NAVEGADOR, que não resolve o host interno do cluster.
    @ConfigProperty(name = "fiapx.s3.public-endpoint")
    String publicEndpoint;

    @ConfigProperty(name = "fiapx.s3.region")
    String region;

    @ConfigProperty(name = "fiapx.s3.access-key")
    String accessKey;

    @ConfigProperty(name = "fiapx.s3.secret-key")
    String secretKey;

    @Produces
    @ApplicationScoped
    public S3Presigner presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(publicEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    void close(@Disposes S3Presigner presigner) {
        presigner.close();
    }
}
