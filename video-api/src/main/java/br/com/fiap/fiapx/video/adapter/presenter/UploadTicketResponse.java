package br.com.fiap.fiapx.video.adapter.presenter;

import br.com.fiap.fiapx.video.usecase.port.in.RequestUploadUseCase;

public record UploadTicketResponse(
        String videoId, String objectKey, String uploadUrl, int expiresInSeconds) {

    public static UploadTicketResponse from(RequestUploadUseCase.UploadTicket ticket) {
        return new UploadTicketResponse(
                ticket.videoId().toString(),
                ticket.objectKey(),
                ticket.uploadUrl(),
                ticket.expiresInSeconds());
    }
}
