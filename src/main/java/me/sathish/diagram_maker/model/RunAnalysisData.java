package me.sathish.diagram_maker.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RunAnalysisData {

    @Size(max = 255)
    private String aiAnalysisData;

    @Size(max = 255)
    private String inputData;

    @Valid
    private FileData runEmoji;

}
