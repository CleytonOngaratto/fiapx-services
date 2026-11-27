package br.com.fiap.fiapx.video.infrastructure.config;

import br.com.fiap.fiapx.video.usecase.interactor.RequestUploadInteractor;
import br.com.fiap.fiapx.video.usecase.port.in.RequestUploadUseCase;
import br.com.fiap.fiapx.video.usecase.port.out.ObjectStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class VideoModuleConfig {

    @Produces
    @ApplicationScoped
    public RequestUploadUseCase requestUpload(ObjectStoragePort storage) {
        return new RequestUploadInteractor(storage);
    }
}
