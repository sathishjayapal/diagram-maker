package me.sathish.diagram_maker.runsai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunAnalysisResponse {

    private UUID documentId;
    private boolean containsRunData;
    private String summary;
    private List<RunInsight> insights;
    private List<String> recommendations;
    private List<String> riskFlags;
    private Integer confidenceScore;
    private PerformanceMetrics metrics;
    private String rawAnalysis;
    private Instant analyzedAt;
    private boolean cachedResult;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunInsight {
        private String category;
        private String observation;
        private String recommendation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private int totalRuns;
        private double totalDistanceMiles;
        private String totalDuration;
        private Double averagePaceMinPerMile;
        private Integer averageHeartRate;
        private Integer totalCalories;
    }

}
