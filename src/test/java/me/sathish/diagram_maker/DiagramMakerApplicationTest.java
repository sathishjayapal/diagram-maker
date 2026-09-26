package me.sathish.diagram_maker;

import io.modelcontextprotocol.server.McpServerFeatures;
import me.sathish.diagram_maker.config.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class DiagramMakerApplicationTest extends BaseIT {

    @Autowired
    @Qualifier("toolSpecs")
    private List<McpServerFeatures.SyncToolSpecification> syncToolSpecifications;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldRegisterGenerateRunDiagramTool() {
        assertThat(syncToolSpecifications)
                .isNotEmpty()
                .anyMatch(tool -> "generate_run_diagram".equals(tool.tool().name()));
    }

}
