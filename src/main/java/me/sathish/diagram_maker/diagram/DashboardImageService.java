package me.sathish.diagram_maker.diagram;

import me.sathish.diagram_maker.runsai.RunAnalysisResponse;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardImageService {

    public static final int WIDTH = 900;
    public static final int HEIGHT = 700;

    public byte[] generateDashboard(final RunAnalysisResponse analysis, final DiagramType type, final ImageFormat format) throws IOException {
        if (format == ImageFormat.SVG) {
            throw new UnsupportedOperationException("SVG format is not yet supported");
        }
        if (format == ImageFormat.JPG) {
            throw new UnsupportedOperationException("JPG format is not yet supported");
        }

        final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        graphics.setColor(Color.BLACK);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int y = drawHeader(graphics, analysis);
        y = drawSummary(graphics, analysis.getSummary(), y);
        y = drawMetricsChart(graphics, analysis.getMetrics(), y);
        y = drawMetricsText(graphics, analysis.getMetrics(), y);
        y = drawInsights(graphics, analysis.getInsights(), y);
        y = drawRiskFlags(graphics, analysis.getRiskFlags(), y);
        drawConfidence(graphics, analysis.getConfidenceScore(), y);

        graphics.dispose();

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private int drawHeader(final Graphics2D graphics, final RunAnalysisResponse analysis) {
        graphics.setFont(new Font("SansSerif", Font.BOLD, 24));
        final String title = "Run Analysis Dashboard";
        drawCenteredString(graphics, title, WIDTH / 2, 40);
        return 70;
    }

    private int drawSummary(final Graphics2D graphics, final String summary, int y) {
        if (summary == null || summary.isBlank()) {
            return y;
        }
        graphics.setFont(new Font("SansSerif", Font.BOLD, 16));
        drawLeftAlignedString(graphics, "Summary", 40, y);
        y += 25;
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return drawWrappedText(graphics, summary, 40, y, WIDTH - 80, 20);
    }

    private int drawMetricsChart(final Graphics2D graphics, final RunAnalysisResponse.PerformanceMetrics metrics, int y) {
        final CategoryChart chart = buildMetricsChart(metrics);
        final BufferedImage chartImage = BitmapEncoder.getBufferedImage(chart);
        final int chartWidth = WIDTH - 80;
        final int chartHeight = 200;
        graphics.drawImage(chartImage, 40, y, chartWidth, chartHeight, null);
        return y + chartHeight + 30;
    }

    private int drawMetricsText(final Graphics2D graphics, final RunAnalysisResponse.PerformanceMetrics metrics, int y) {
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return drawWrappedText(graphics, formatMetrics(metrics), 40, y, WIDTH - 80, 20);
    }

    private int drawInsights(final Graphics2D graphics, final List<RunAnalysisResponse.RunInsight> insights, int y) {
        if (insights == null || insights.isEmpty()) {
            return y;
        }
        graphics.setFont(new Font("SansSerif", Font.BOLD, 16));
        drawLeftAlignedString(graphics, "Insights", 40, y);
        y += 25;
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
        for (final RunAnalysisResponse.RunInsight insight : insights) {
            final String line = String.format("- %s: %s (%s)",
                    insight.getCategory(), insight.getObservation(), insight.getRecommendation());
            y = drawWrappedText(graphics, line, 50, y, WIDTH - 100, 20);
        }
        return y + 10;
    }

    private int drawRiskFlags(final Graphics2D graphics, final List<String> riskFlags, int y) {
        if (riskFlags == null || riskFlags.isEmpty()) {
            return y;
        }
        graphics.setFont(new Font("SansSerif", Font.BOLD, 16));
        drawLeftAlignedString(graphics, "Risk Flags", 40, y);
        y += 25;
        graphics.setColor(Color.RED);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
        for (final String flag : riskFlags) {
            y = drawWrappedText(graphics, "- " + flag, 50, y, WIDTH - 100, 20);
        }
        graphics.setColor(Color.BLACK);
        return y + 10;
    }

    private void drawConfidence(final Graphics2D graphics, final Integer confidenceScore, int y) {
        if (confidenceScore == null) {
            return;
        }
        graphics.setFont(new Font("SansSerif", Font.BOLD, 16));
        drawLeftAlignedString(graphics, "Confidence: " + confidenceScore + "%", 40, y);
    }

    private CategoryChart buildMetricsChart(final RunAnalysisResponse.PerformanceMetrics metrics) {
        final CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(200)
                .title("Key Metrics")
                .xAxisTitle("Metric")
                .yAxisTitle("Value")
                .build();
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setAxisTitleFont(new Font("SansSerif", Font.PLAIN, 12));
        chart.getStyler().setAxisTickLabelsFont(new Font("SansSerif", Font.PLAIN, 10));

        final List<String> categories = new ArrayList<>();
        final List<Number> values = new ArrayList<>();
        if (metrics != null) {
            addMetric(categories, values, "Distance (mi)", metrics.getTotalDistanceMiles());
            addMetric(categories, values, "Avg HR", metrics.getAverageHeartRate());
            addMetric(categories, values, "Calories", metrics.getTotalCalories());
        }

        if (!categories.isEmpty()) {
            chart.addSeries("Metrics", categories, values);
        }
        return chart;
    }

    private void addMetric(final List<String> categories, final List<Number> values, final String label, final Number value) {
        if (value != null) {
            categories.add(label);
            values.add(value);
        }
    }

    private String formatMetrics(final RunAnalysisResponse.PerformanceMetrics metrics) {
        if (metrics == null) {
            return "No metrics available";
        }
        return String.format(
                "Runs: %d | Distance: %.2f mi | Duration: %s | Avg Pace: %s min/mi | Avg HR: %d | Calories: %d",
                metrics.getTotalRuns(),
                metrics.getTotalDistanceMiles(),
                metrics.getTotalDuration() != null ? metrics.getTotalDuration() : "N/A",
                metrics.getAveragePaceMinPerMile() != null ? String.format("%.2f", metrics.getAveragePaceMinPerMile()) : "N/A",
                metrics.getAverageHeartRate() != null ? metrics.getAverageHeartRate() : 0,
                metrics.getTotalCalories() != null ? metrics.getTotalCalories() : 0);
    }

    private void drawCenteredString(final Graphics2D graphics, final String text, final int x, final int y) {
        final FontMetrics metrics = graphics.getFontMetrics();
        final int width = metrics.stringWidth(text);
        graphics.drawString(text, x - width / 2, y);
    }

    private void drawLeftAlignedString(final Graphics2D graphics, final String text, final int x, final int y) {
        graphics.drawString(text, x, y);
    }

    private int drawWrappedText(final Graphics2D graphics, final String text, final int x, int y, final int maxWidth, final int lineHeight) {
        final FontMetrics metrics = graphics.getFontMetrics();
        final String[] words = text.split(" ");
        final StringBuilder line = new StringBuilder();
        for (final String word : words) {
            final String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                graphics.drawString(line.toString(), x, y);
                y += lineHeight;
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (!line.isEmpty()) {
            graphics.drawString(line.toString(), x, y);
            y += lineHeight;
        }
        return y;
    }

}
