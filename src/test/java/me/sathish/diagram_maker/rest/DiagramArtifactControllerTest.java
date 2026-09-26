package me.sathish.diagram_maker.rest;

import me.sathish.diagram_maker.diagram.DiagramStorageService;
import me.sathish.diagram_maker.diagram.InvalidDiagramPathException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagramArtifactControllerTest {

    @Mock
    private DiagramStorageService diagramStorageService;

    @InjectMocks
    private DiagramArtifactController controller;

    @Test
    void shouldDownloadStoredArtifactInline() throws Exception {
        final Path artifact = Files.createTempFile("diagram-", ".png");
        Files.writeString(artifact, "image");
        when(diagramStorageService.resolveForRead("analysis.png")).thenReturn(artifact);

        final ResponseEntity<Resource> response = controller.download("analysis.png");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("inline");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(5);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldReturnNotFoundForMissingArtifact() throws Exception {
        when(diagramStorageService.resolveForRead("missing.png")).thenReturn(null);

        assertThat(controller.download("missing.png").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectInvalidArtifactPath() throws Exception {
        when(diagramStorageService.resolveForRead("escape.png"))
                .thenThrow(new InvalidDiagramPathException("invalid"));

        assertThat(controller.download("escape.png").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
