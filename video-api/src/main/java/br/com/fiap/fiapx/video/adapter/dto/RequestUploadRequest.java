package br.com.fiap.fiapx.video.adapter.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestUploadRequest(
        @NotBlank String ownerId,
        @NotBlank String filename,
        @NotBlank String contentType) {}
