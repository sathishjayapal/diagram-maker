package me.sathish.diagram_maker.diagram;

import me.sathish.diagram_maker.runsai.RunAnalysisResponse;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardImageServiceTest {

    private final DashboardImageService service = new DashboardImageService();

    @Test
    void shouldGeneratePngDashboard() throws Exception {
        final RunAnalysisResponse analysis = RunAnalysisResponse.builder()
                .documentId(UUID.randomUUID())
                .summary("Steady aerobic work with effective intensity distribution.")
                .insights(List.of(
                        RunAnalysisResponse.RunInsight.builder()
                                .category("pace")
                                .observation("controlled")
                                .recommendation("maintain easy pace")
                                .build()))
                .recommendations(List.of("Keep easy pace"))
                .riskFlags(List.of("Watch recovery"))
                .confidenceScore(86)
                .metrics(RunAnalysisResponse.PerformanceMetrics.builder()
                        .totalRuns(1)
                        .totalDistanceMiles(5.5)
                        .totalDuration("00:28:30")
                        .averagePaceMinPerMile(5.18)
                        .averageHeartRate(165)
                        .totalCalories(450)
                        .build())
                .analyzedAt(Instant.now())
                .build();

        final byte[] imageBytes = service.generateDashboard(analysis, DiagramType.SUMMARY, ImageFormat.PNG);

        assertThat(imageBytes).isNotEmpty();
        assertThat(imageBytes[0]).isEqualTo((byte) 0x89);
        assertThat(imageBytes[1]).isEqualTo((byte) 'P');
        assertThat(imageBytes[2]).isEqualTo((byte) 'N');
        assertThat(imageBytes[3]).isEqualTo((byte) 'G');

        final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(DashboardImageService.WIDTH);
        assertThat(image.getHeight()).isEqualTo(DashboardImageService.HEIGHT);
    }

    @Test
    void shouldRejectUnsupportedSvgFormat() {
        final RunAnalysisResponse analysis = RunAnalysisResponse.builder().build();

        assertThatThrownBy(() -> service.generateDashboard(analysis, DiagramType.SUMMARY, ImageFormat.SVG))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("SVG");
    }

}
