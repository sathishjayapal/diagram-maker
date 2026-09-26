package me.sathish.diagram_maker.diagram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiagramStorageServiceTest {

    private DiagramStorageService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        final DiagramProperties properties = new DiagramProperties();
        properties.setOutputDir(tempDir.toString());
        service = new DiagramStorageService(properties);
    }

    @Test
    void shouldGenerateDefaultFileWhenNoOutputPathProvided() throws IOException {
        final byte[] content = "test".getBytes();

        final DiagramFile file = service.save(content, null, ImageFormat.PNG);

        assertThat(file.fileName()).endsWith(".png");
        assertThat(Path.of(file.filePath())).exists();
        assertThat(file.contentType()).isEqualTo("image/png");
        assertThat(file.sizeBytes()).isEqualTo(content.length);
    }

    @Test
    void shouldSaveToRelativePath() throws IOException {
        final byte[] content = "test".getBytes();

        final DiagramFile file = service.save(content, "my-run.png", ImageFormat.PNG);

        assertThat(file.fileName()).isEqualTo("my-run.png");
        assertThat(Path.of(file.filePath())).exists();
        assertThat(Path.of(file.filePath()).getParent()).isEqualTo(tempDir);
    }

    @Test
    void shouldSaveToAbsolutePathUnderRoot() throws IOException {
        final byte[] content = "test".getBytes();
        final Path target = tempDir.resolve("nested/run.png");

        final DiagramFile file = service.save(content, target.toString(), ImageFormat.PNG);

        assertThat(Path.of(file.filePath())).exists();
    }

    @Test
    void shouldRejectRelativePathTraversal() {
        final byte[] content = "test".getBytes();

        assertThatThrownBy(() -> service.save(content, "../escape.png", ImageFormat.PNG))
                .isInstanceOf(InvalidDiagramPathException.class)
                .hasMessageContaining("escapes configured diagram directory");
    }

    @Test
    void shouldRejectAbsolutePathOutsideRoot() {
        final byte[] content = "test".getBytes();

        assertThatThrownBy(() -> service.save(content, "/etc/passwd.png", ImageFormat.PNG))
                .isInstanceOf(InvalidDiagramPathException.class)
                .hasMessageContaining("must be within configured diagram directory");
    }

}
