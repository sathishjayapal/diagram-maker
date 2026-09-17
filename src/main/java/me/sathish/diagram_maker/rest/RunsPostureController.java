package me.sathish.diagram_maker.rest;

import jakarta.validation.Valid;
import me.sathish.diagram_maker.model.RunAnalysisData;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/getImageAnalysis", produces = MediaType.APPLICATION_JSON_VALUE)
public class RunsPostureController {

    @PostMapping("/")
    public ResponseEntity<RunAnalysisData> getRunAnalysis(
            @RequestBody @Valid final RunAnalysisData runAnalysisData) {
        return ResponseEntity.ok(null);
    }

}
