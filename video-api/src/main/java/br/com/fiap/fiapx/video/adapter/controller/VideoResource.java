package br.com.fiap.fiapx.video.adapter.controller;

import br.com.fiap.fiapx.video.adapter.dto.RequestUploadRequest;
import br.com.fiap.fiapx.video.adapter.presenter.UploadTicketResponse;
import br.com.fiap.fiapx.video.usecase.port.in.RequestUploadUseCase;
import br.com.fiap.fiapx.video.usecase.port.out.ObjectStoragePort;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/videos")
@Produces(MediaType.APPLICATION_JSON)
public class VideoResource {

    private final RequestUploadUseCase requestUpload;
    private final ObjectStoragePort storage;

    public VideoResource(RequestUploadUseCase requestUpload, ObjectStoragePort storage) {
        this.requestUpload = requestUpload;
        this.storage = storage;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response request(@Valid RequestUploadRequest body) {
        RequestUploadUseCase.UploadTicket ticket = requestUpload.execute(
                new RequestUploadUseCase.Command(body.ownerId(), body.filename(), body.contentType()));

        return Response.accepted(UploadTicketResponse.from(ticket))
                .location(URI.create("/videos/" + ticket.videoId()))
                .build();
    }

    @GET
    @Path("/uploaded/{ownerId}/{videoId}/{extension}")
    public Response uploaded(
            @PathParam("ownerId") String ownerId,
            @PathParam("videoId") String videoId,
            @PathParam("extension") String extension) {
        String key = "videos/%s/%s/source.%s".formatted(ownerId, videoId, extension);
        return storage.exists(key)
                ? Response.ok(java.util.Map.of("objectKey", key, "uploaded", true)).build()
                : Response.status(Response.Status.NOT_FOUND)
                        .entity(java.util.Map.of("objectKey", key, "uploaded", false)).build();
    }
}
