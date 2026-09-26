package me.sathish.diagram_maker.rest;

import lombok.RequiredArgsConstructor;
import me.sathish.diagram_maker.diagram.DiagramStorageService;
import me.sathish.diagram_maker.diagram.InvalidDiagramPathException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/diagrams")
@RequiredArgsConstructor
public class DiagramArtifactController {

    private final DiagramStorageService diagramStorageService;

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable final String fileName) throws IOException {
        try {
            final Path artifact = diagramStorageService.resolveForRead(fileName);
            if (artifact == null) {
                return ResponseEntity.notFound().build();
            }

            final MediaType contentType = MediaTypeFactory.getMediaType(fileName)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            final ContentDisposition disposition = ContentDisposition.inline()
                    .filename(fileName)
                    .build();
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .contentLength(artifact.toFile().length())
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(new FileSystemResource(artifact));
        } catch (InvalidDiagramPathException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
