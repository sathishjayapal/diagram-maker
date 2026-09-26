package me.sathish.diagram_maker.runsai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "runs-ai-analyzer")
@Data
public class RunsAiAnalyzerProperties {

    @NotBlank
    private String baseUrl;

}
