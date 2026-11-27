package br.com.fiap.fiapx.video.adapter.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.fiap.fiapx.video.usecase.port.out.ObjectStoragePort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class VideoResourceTest {

    @InjectMock
    ObjectStoragePort storage;

    @Test
    void accepts_the_request_and_returns_the_upload_ticket() {
        when(storage.presignUpload(any(), any()))
                .thenReturn(new ObjectStoragePort.PresignedUpload("http://localhost:9090/signed", 900));

        given().contentType(ContentType.JSON)
                .body("{\"ownerId\":\"rm368677\",\"filename\":\"palestra.mp4\",\"contentType\":\"video/mp4\"}")
                .when().post("/videos")
                .then()
                .statusCode(202)
                .header("Location", containsString("/videos/"))
                .body("uploadUrl", is("http://localhost:9090/signed"))
                .body("objectKey", startsWith("videos/rm368677/"))
                .body("expiresInSeconds", is(900));
    }

    @Test
    void rejects_unsupported_format_with_400() {
        given().contentType(ContentType.JSON)
                .body("{\"ownerId\":\"rm368677\",\"filename\":\"documento.pdf\",\"contentType\":\"application/pdf\"}")
                .when().post("/videos")
                .then()
                .statusCode(400)
                .body("error", is("invalid_request"));
    }

    @Test
    void rejects_blank_fields_with_400() {
        given().contentType(ContentType.JSON)
                .body("{\"ownerId\":\"\",\"filename\":\"a.mp4\",\"contentType\":\"video/mp4\"}")
                .when().post("/videos")
                .then().statusCode(400);
    }

    @Test
    void reports_not_found_when_the_object_was_not_uploaded() {
        when(storage.exists(any())).thenReturn(false);

        given().when().get("/videos/uploaded/rm368677/11111111-2222-3333-4444-555555555555/mp4")
                .then().statusCode(404).body("uploaded", is(false));
    }

    @Test
    void reports_ok_when_the_object_is_there() {
        when(storage.exists(any())).thenReturn(true);

        given().when().get("/videos/uploaded/rm368677/11111111-2222-3333-4444-555555555555/mp4")
                .then().statusCode(200).body("uploaded", is(true));
    }

    @Test
    void health_is_up() {
        given().when().get("/q/health/ready").then().statusCode(200);
    }
}
