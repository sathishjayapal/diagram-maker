package me.sathish.diagram_maker.rest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import me.sathish.diagram_maker.config.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;


public class RunsPostureControllerTest extends BaseIT {

    @Test
    void getRunAnalysis_success() {
        prepareUpload("f4480bed-f1a6-347a-acd5-54e0d7767bcd", "testFile.pdf");
        RestAssured
                .given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/runAnalysisDataRequest.json"))
                .when()
                    .post("/getImageAnalysis/")
                .then()
                    .statusCode(HttpStatus.OK.value());
    }

}
