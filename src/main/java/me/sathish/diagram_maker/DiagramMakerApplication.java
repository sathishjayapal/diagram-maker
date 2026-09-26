package me.sathish.diagram_maker;

import me.sathish.diagram_maker.diagram.DiagramProperties;
import me.sathish.diagram_maker.runsai.RunsAiAnalyzerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties({RunsAiAnalyzerProperties.class, DiagramProperties.class})
public class DiagramMakerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(DiagramMakerApplication.class, args);
    }

}
