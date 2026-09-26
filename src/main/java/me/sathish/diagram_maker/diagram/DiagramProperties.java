package me.sathish.diagram_maker.diagram;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "diagram")
@Data
public class DiagramProperties {

    @NotBlank
    private String outputDir;

}
