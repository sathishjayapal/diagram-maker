package me.sathish.diagram_maker.runsai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunAnalysisDocumentResponse {

    private Long id;
    private UUID documentId;
    private String activityIds;
    private String queryText;
    private String analysisContent;
    private String summary;
    private Integer totalRuns;
    private Double totalDistanceMiles;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

}
