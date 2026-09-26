package me.sathish.diagram_maker.runsai;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RunsAiAnalyzerClientTest {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer mockServer;
    private RunsAiAnalyzerClient client;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        final RunsAiAnalyzerProperties properties = new RunsAiAnalyzerProperties();
        properties.setBaseUrl("http://localhost:8081");
        client = new RunsAiAnalyzerClient(restTemplate, properties);
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void shouldFetchAnalysisByDocumentId() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final RunAnalysisDocumentResponse document = RunAnalysisDocumentResponse.builder()
                .documentId(documentId)
                .activityIds("12345")
                .summary("Good run")
                .totalRuns(1)
                .totalDistanceMiles(5.5)
                .metadata(Map.of(
                        "totalDuration", "00:28:30",
                        "averagePace", 5.18,
                        "averageHeartRate", 165,
                        "totalCalories", 450,
                        "confidenceScore", 86,
                        "recommendations", List.of("Keep easy pace"),
                        "riskFlags", List.of(),
                        "insights", List.of(Map.of("category", "pace", "observation", "steady", "recommendation", "maintain"))
                ))
                .createdAt(LocalDateTime.now())
                .build();

        mockServer.expect(requestTo("http://localhost:8081/api/v1/rag/document/" + documentId))
                .andRespond(withSuccess(objectMapper.writeValueAsString(document), MediaType.APPLICATION_JSON));

        final RunAnalysisResponse response = client.getAnalysisByDocumentId(documentId);

        assertThat(response.getDocumentId()).isEqualTo(documentId);
        assertThat(response.getSummary()).isEqualTo("Good run");
        assertThat(response.getMetrics().getTotalDistanceMiles()).isEqualTo(5.5);
        assertThat(response.getInsights()).hasSize(1);
        assertThat(response.getInsights().getFirst().getCategory()).isEqualTo("pace");
    }

    @Test
    void shouldFetchAnalysisByActivityId() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final RunAnalysisDocumentResponse[] documents = new RunAnalysisDocumentResponse[]{
                RunAnalysisDocumentResponse.builder()
                        .documentId(documentId)
                        .activityIds("12345")
                        .summary("Good run")
                        .totalRuns(1)
                        .totalDistanceMiles(5.5)
                        .metadata(Map.of(
                                "totalDuration", "00:28:30",
                                "averagePace", 5.18,
                                "averageHeartRate", 165,
                                "totalCalories", 450,
                                "confidenceScore", 86,
                                "recommendations", List.of("Keep easy pace"),
                                "riskFlags", List.of(),
                                "insights", List.of(Map.of("category", "pace", "observation", "steady", "recommendation", "maintain"))
                        ))
                        .createdAt(LocalDateTime.now())
                        .build()
        };

        mockServer.expect(requestTo("http://localhost:8081/api/v1/rag/activity/12345"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(documents), MediaType.APPLICATION_JSON));

        final RunAnalysisResponse response = client.getAnalysisByActivityId("12345");

        assertThat(response.getDocumentId()).isEqualTo(documentId);
        assertThat(response.getMetrics().getTotalDistanceMiles()).isEqualTo(5.5);
    }

    @Test
    void shouldThrowWhenNoActivityAnalysisFound() {
        mockServer.expect(requestTo("http://localhost:8081/api/v1/rag/activity/99999"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getAnalysisByActivityId("99999"))
                .isInstanceOf(AnalysisNotFoundException.class)
                .hasMessageContaining("No analysis found for activity 99999");
    }

}
