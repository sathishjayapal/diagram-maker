package me.sathish.diagram_maker.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import me.sathish.diagram_maker.config.BaseIT;
import me.sathish.diagram_maker.model.FileData;
import me.sathish.diagram_maker.service.FileDataService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;


public class FileUploadResourceTest extends BaseIT {

    @Test
    public void fileUpload_success() throws Exception {
        final String resultStr = RestAssured
                .given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.MULTIPART)
                    .multiPart(testFile.getFile())
                .when()
                    .post("/fileUpload")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .asString();
        final FileData result = objectMapper.readValue(resultStr, FileData.class);
        final File uploadFile = new File(FileDataService.UPLOAD_DIRECTORY + "/" + result.getUid() + "/" + result.getFileName());
        assertTrue(uploadFile.exists());
        assertEquals(testFile.getContentAsString(StandardCharsets.UTF_8), Files.readString(uploadFile.toPath()));
    }

}
