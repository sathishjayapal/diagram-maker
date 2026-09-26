package me.sathish.diagram_maker.diagram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.sathish.diagram_maker.runsai.RunAnalysisResponse;
import me.sathish.diagram_maker.runsai.RunsAiAnalyzerClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagramMakerMcpTools {

    private final RunsAiAnalyzerClient runsAiAnalyzerClient;
    private final DashboardImageService dashboardImageService;
    private final DiagramStorageService diagramStorageService;

    @McpTool(name = "generate_run_diagram",
            description = "Generates a summary dashboard image for a run based on the analysis data stored in runs-ai-analyzer. " +
                    "The activityId identifies the run. The returned object contains the file path and metadata.")
    public RunDiagramResult generateRunDiagram(
            @McpToolParam(description = "The activity ID of the run to visualize", required = true) final String activityId,
            @McpToolParam(description = "Type of diagram to generate. Default is SUMMARY.", required = false) final DiagramType diagramType,
            @McpToolParam(description = "Output image format. Default is PNG.", required = false) final ImageFormat format,
            @McpToolParam(description = "Optional output file path. If omitted, a unique file is created in the configured diagram directory.", required = false) final String outputPath
    ) throws IOException {
        final DiagramType effectiveType = diagramType != null ? diagramType : DiagramType.SUMMARY;
        final ImageFormat effectiveFormat = format != null ? format : ImageFormat.PNG;

        log.info("Generating {} diagram for activity {} in {} format", effectiveType, activityId, effectiveFormat);
        final RunAnalysisResponse analysis = runsAiAnalyzerClient.getAnalysisByActivityId(activityId);
        final byte[] imageBytes = dashboardImageService.generateDashboard(analysis, effectiveType, effectiveFormat);
        final DiagramFile saved = diagramStorageService.save(imageBytes, outputPath, effectiveFormat);

        return new RunDiagramResult(saved.uid(), saved.fileName(), saved.filePath(), saved.contentType(), saved.sizeBytes());
    }

    @McpTool(name = "generate_analysis_diagram",
            description = "Generates a dashboard image from a stored runs-ai-analyzer document. " +
                    "The documentId identifies the completed analysis. The returned object contains artifact metadata.")
    public RunDiagramResult generateAnalysisDiagram(
            @McpToolParam(description = "Document ID of the completed analysis", required = true) final UUID documentId,
            @McpToolParam(description = "Type of diagram to generate. Default is SUMMARY.", required = false) final DiagramType diagramType,
            @McpToolParam(description = "Output image format. Default is PNG.", required = false) final ImageFormat format
    ) throws IOException {
        final DiagramType effectiveType = diagramType != null ? diagramType : DiagramType.SUMMARY;
        final ImageFormat effectiveFormat = format != null ? format : ImageFormat.PNG;

        log.info("Generating {} diagram for analysis document {} in {} format", effectiveType, documentId, effectiveFormat);
        final RunAnalysisResponse analysis = runsAiAnalyzerClient.getAnalysisByDocumentId(documentId);
        final byte[] imageBytes = dashboardImageService.generateDashboard(analysis, effectiveType, effectiveFormat);
        final DiagramFile saved = diagramStorageService.save(imageBytes, null, effectiveFormat);

        return new RunDiagramResult(saved.uid(), saved.fileName(), saved.filePath(), saved.contentType(), saved.sizeBytes());
    }

}
