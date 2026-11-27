package br.com.fiap.fiapx.video.usecase.interactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.fiapx.video.usecase.port.in.RequestUploadUseCase;
import br.com.fiap.fiapx.video.usecase.port.out.ObjectStoragePort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RequestUploadInteractorTest {

    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final RequestUploadInteractor interactor = new RequestUploadInteractor(storage);

    @Test
    void presigns_a_key_scoped_by_owner_and_a_fresh_video_id() {
        when(storage.presignUpload(any(), eq("video/mp4")))
                .thenReturn(new ObjectStoragePort.PresignedUpload("http://localhost:9090/signed", 900));

        RequestUploadUseCase.UploadTicket ticket = interactor.execute(
                new RequestUploadUseCase.Command("rm368677", "palestra.mp4", "video/mp4"));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).presignUpload(key.capture(), eq("video/mp4"));

        assertEquals("videos/rm368677/%s/source.mp4".formatted(ticket.videoId()), key.getValue());
        assertEquals(key.getValue(), ticket.objectKey());
        assertEquals("http://localhost:9090/signed", ticket.uploadUrl());
        assertEquals(900, ticket.expiresInSeconds());
    }

    @Test
    void gives_a_different_video_id_to_each_request() {
        when(storage.presignUpload(any(), any()))
                .thenReturn(new ObjectStoragePort.PresignedUpload("http://x/signed", 900));

        var first = interactor.execute(new RequestUploadUseCase.Command("rm1", "a.mp4", "video/mp4"));
        var second = interactor.execute(new RequestUploadUseCase.Command("rm1", "a.mp4", "video/mp4"));

        assertTrue(!first.videoId().equals(second.videoId()));
    }

    @Test
    void rejects_unsupported_format_without_touching_the_storage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> interactor.execute(
                        new RequestUploadUseCase.Command("rm368677", "documento.pdf", "application/pdf")));
        verifyNoInteractions(storage);
    }
}
