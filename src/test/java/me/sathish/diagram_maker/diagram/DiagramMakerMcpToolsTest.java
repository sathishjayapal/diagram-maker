package me.sathish.diagram_maker.diagram;

import me.sathish.diagram_maker.runsai.RunAnalysisResponse;
import me.sathish.diagram_maker.runsai.RunsAiAnalyzerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiagramMakerMcpToolsTest {

    @Mock
    private RunsAiAnalyzerClient client;

    @Mock
    private DashboardImageService imageService;

    @Mock
    private DiagramStorageService storageService;

    @InjectMocks
    private DiagramMakerMcpTools tools;

    @Test
    void shouldGenerateRunDiagram() throws Exception {
        final String activityId = "12345";
        final RunAnalysisResponse analysis = RunAnalysisResponse.builder()
                .documentId(UUID.randomUUID())
                .summary("Good run")
                .analyzedAt(Instant.now())
                .build();
        final byte[] imageBytes = new byte[]{0x01, 0x02, 0x03};
        final DiagramFile saved = new DiagramFile(
                "uid-1", "run.png", "/tmp/run.png", "image/png", imageBytes.length);

        when(client.getAnalysisByActivityId(activityId)).thenReturn(analysis);
        when(imageService.generateDashboard(analysis, DiagramType.SUMMARY, ImageFormat.PNG)).thenReturn(imageBytes);
        when(storageService.save(imageBytes, null, ImageFormat.PNG)).thenReturn(saved);

        final RunDiagramResult result = tools.generateRunDiagram(activityId, DiagramType.SUMMARY, ImageFormat.PNG, null);

        assertThat(result.uid()).isEqualTo("uid-1");
        assertThat(result.fileName()).isEqualTo("run.png");
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.sizeBytes()).isEqualTo(imageBytes.length);

        verify(client).getAnalysisByActivityId(activityId);
        verify(imageService).generateDashboard(analysis, DiagramType.SUMMARY, ImageFormat.PNG);
        verify(storageService).save(imageBytes, null, ImageFormat.PNG);
    }

    @Test
    void shouldUseDefaultsWhenOptionalParametersAreNull() throws Exception {
        final String activityId = "12345";
        final RunAnalysisResponse analysis = RunAnalysisResponse.builder()
                .summary("Good run")
                .analyzedAt(Instant.now())
                .build();
        final byte[] imageBytes = new byte[]{0x01, 0x02};
        final DiagramFile saved = new DiagramFile(
                "uid-2", "run.png", "/tmp/run.png", "image/png", imageBytes.length);

        when(client.getAnalysisByActivityId(activityId)).thenReturn(analysis);
        when(imageService.generateDashboard(analysis, DiagramType.SUMMARY, ImageFormat.PNG)).thenReturn(imageBytes);
        when(storageService.save(imageBytes, null, ImageFormat.PNG)).thenReturn(saved);

        final RunDiagramResult result = tools.generateRunDiagram(activityId, null, null, null);

        assertThat(result.uid()).isEqualTo("uid-2");
        verify(imageService).generateDashboard(analysis, DiagramType.SUMMARY, ImageFormat.PNG);
    }

    @Test
    void shouldGenerateAnalysisDiagramByDocumentId() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final RunAnalysisResponse analysis = RunAnalysisResponse.builder()
                .documentId(documentId)
                .summary("Ten-run analysis")
                .analyzedAt(Instant.now())
                .build();
        final byte[] imageBytes = new byte[]{0x01, 0x02, 0x03};
        final DiagramFile saved = new DiagramFile(
                "uid-3", "analysis.svg", "/tmp/analysis.svg", "image/svg+xml", imageBytes.length);

        when(client.getAnalysisByDocumentId(documentId)).thenReturn(analysis);
        when(imageService.generateDashboard(analysis, DiagramType.ALL_METRICS, ImageFormat.SVG)).thenReturn(imageBytes);
        when(storageService.save(imageBytes, null, ImageFormat.SVG)).thenReturn(saved);

        final RunDiagramResult result = tools.generateAnalysisDiagram(
                documentId, DiagramType.ALL_METRICS, ImageFormat.SVG);

        assertThat(result.uid()).isEqualTo("uid-3");
        assertThat(result.fileName()).isEqualTo("analysis.svg");
        assertThat(result.filePath()).isEqualTo("/tmp/analysis.svg");
        assertThat(result.contentType()).isEqualTo("image/svg+xml");
        assertThat(result.sizeBytes()).isEqualTo(imageBytes.length);
        verify(client).getAnalysisByDocumentId(documentId);
        verify(imageService).generateDashboard(analysis, DiagramType.ALL_METRICS, ImageFormat.SVG);
        verify(storageService).save(imageBytes, null, ImageFormat.SVG);
    }

    @Test
    void shouldUseDefaultsForAnalysisDiagram() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final RunAnalysisResponse analysis = RunAnalysisResponse.builder()
                .documentId(documentId)
                .summary("Ten-run analysis")
                .build();
        final byte[] imageBytes = new byte[]{0x01};
        final DiagramFile saved = new DiagramFile(
                "uid-4", "analysis.png", "/tmp/analysis.png", "image/png", imageBytes.length);

        when(client.getAnalysisByDocumentId(documentId)).thenReturn(analysis);
        when(imageService.generateDashboard(analysis, DiagramType.SUMMARY, ImageFormat.PNG)).thenReturn(imageBytes);
        when(storageService.save(imageBytes, null, ImageFormat.PNG)).thenReturn(saved);

        final RunDiagramResult result = tools.generateAnalysisDiagram(documentId, null, null);

        assertThat(result.uid()).isEqualTo("uid-4");
        verify(imageService).generateDashboard(analysis, DiagramType.SUMMARY, ImageFormat.PNG);
        verify(storageService).save(imageBytes, null, ImageFormat.PNG);
    }

}
