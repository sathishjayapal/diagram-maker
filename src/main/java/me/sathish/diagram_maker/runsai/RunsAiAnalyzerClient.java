package me.sathish.diagram_maker.runsai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunsAiAnalyzerClient {

    private final RestTemplate restTemplate;
    private final RunsAiAnalyzerProperties properties;

    public RunAnalysisResponse getAnalysisByActivityId(final String activityId) {
        final URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/api/v1/rag/activity/{activityId}")
                .buildAndExpand(activityId)
                .toUri();

        log.info("Fetching analyses for activity {} from {}", activityId, uri);
        final RunAnalysisDocumentResponse[] documents = restTemplate.getForObject(uri, RunAnalysisDocumentResponse[].class);
        if (documents == null || documents.length == 0) {
            throw new AnalysisNotFoundException("No analysis found for activity " + activityId);
        }
        return toRunAnalysisResponse(documents[0]);
    }

    public RunAnalysisResponse getAnalysisByDocumentId(final UUID documentId) {
        final URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/api/v1/rag/document/{documentId}")
                .buildAndExpand(documentId)
                .toUri();

        log.info("Fetching analysis document {} from {}", documentId, uri);
        final RunAnalysisDocumentResponse document = restTemplate.getForObject(uri, RunAnalysisDocumentResponse.class);
        if (document == null) {
            throw new AnalysisNotFoundException("Analysis not found for document " + documentId);
        }
        return toRunAnalysisResponse(document);
    }

    private RunAnalysisResponse toRunAnalysisResponse(final RunAnalysisDocumentResponse document) {
        final Map<String, Object> metadata = document.getMetadata() != null ? document.getMetadata() : Collections.emptyMap();

        return RunAnalysisResponse.builder()
                .documentId(document.getDocumentId())
                .containsRunData(true)
                .summary(document.getSummary())
                .insights(extractInsights(metadata))
                .recommendations(extractStringList(metadata, "recommendations"))
                .riskFlags(extractStringList(metadata, "riskFlags"))
                .confidenceScore(extractInteger(metadata, "confidenceScore"))
                .metrics(extractMetrics(document, metadata))
                .rawAnalysis(document.getAnalysisContent())
                .analyzedAt(document.getCreatedAt() != null ? document.getCreatedAt().toInstant(ZoneOffset.UTC) : null)
                .cachedResult(true)
                .build();
    }

    private RunAnalysisResponse.PerformanceMetrics extractMetrics(final RunAnalysisDocumentResponse document,
                                                                   final Map<String, Object> metadata) {
        final Integer totalRuns = extractInteger(metadata, "totalRuns") != null
                ? extractInteger(metadata, "totalRuns")
                : document.getTotalRuns();
        final Double totalDistanceMiles = extractDouble(metadata, "totalDistanceMiles") != null
                ? extractDouble(metadata, "totalDistanceMiles")
                : document.getTotalDistanceMiles();
        return RunAnalysisResponse.PerformanceMetrics.builder()
                .totalRuns(totalRuns != null ? totalRuns : 0)
                .totalDistanceMiles(totalDistanceMiles != null ? totalDistanceMiles : 0.0)
                .totalDuration(extractString(metadata, "totalDuration"))
                .averagePaceMinPerMile(extractDouble(metadata, "averagePace"))
                .averageHeartRate(extractInteger(metadata, "averageHeartRate"))
                .totalCalories(extractInteger(metadata, "totalCalories"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<RunAnalysisResponse.RunInsight> extractInsights(final Map<String, Object> metadata) {
        final Object raw = metadata.get("insights");
        if (!(raw instanceof final List<?> rawList)) {
            return Collections.emptyList();
        }
        try {
            return rawList.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> (Map<String, Object>) item)
                    .map(map -> RunAnalysisResponse.RunInsight.builder()
                            .category(extractString(map, "category"))
                            .observation(extractString(map, "observation"))
                            .recommendation(extractString(map, "recommendation"))
                            .build())
                    .toList();
        } catch (final Exception e) {
            log.warn("Failed to parse insights from metadata", e);
            return Collections.emptyList();
        }
    }

    private List<String> extractStringList(final Map<String, Object> metadata, final String key) {
        final Object raw = metadata.get(key);
        if (raw instanceof final List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .toList();
        }
        return Collections.emptyList();
    }

    private Integer extractInteger(final Map<String, Object> metadata, final String key) {
        final Object raw = metadata.get(key);
        if (raw instanceof final Number number) {
            return number.intValue();
        }
        return null;
    }

    private Double extractDouble(final Map<String, Object> metadata, final String key) {
        final Object raw = metadata.get(key);
        if (raw instanceof final Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private String extractString(final Map<String, Object> metadata, final String key) {
        final Object raw = metadata.get(key);
        return raw != null ? raw.toString() : null;
    }

}
