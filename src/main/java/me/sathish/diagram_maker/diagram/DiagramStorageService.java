package me.sathish.diagram_maker.diagram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagramStorageService {

    private final DiagramProperties properties;

    public DiagramFile save(final byte[] content, final String requestedPath, final ImageFormat format) throws IOException {
        final Path root = Path.of(properties.getOutputDir()).toAbsolutePath().normalize();

        final Path target;
        if (requestedPath == null || requestedPath.isBlank()) {
            final String fileName = UUID.randomUUID() + "." + format.name().toLowerCase();
            target = root.resolve(fileName);
        } else {
            final Path requested = Path.of(requestedPath).normalize();
            if (requested.isAbsolute()) {
                if (!requested.startsWith(root)) {
                    throw new InvalidDiagramPathException("Absolute output path must be within configured diagram directory: " + root);
                }
                target = requested;
            } else {
                final Path resolved = root.resolve(requested).normalize();
                if (!resolved.startsWith(root)) {
                    throw new InvalidDiagramPathException("Relative output path escapes configured diagram directory: " + root);
                }
                target = resolved;
            }
        }

        Files.createDirectories(target.getParent());
        Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        final String uid = UUID.randomUUID().toString();
        log.info("Saved diagram to {} (uid={})", target, uid);
        return new DiagramFile(uid, target.getFileName().toString(), target.toString(), format.getContentType(), content.length);
    }

    public Path resolveForRead(final String fileName) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidDiagramPathException("Diagram file name is required");
        }

        final Path requested = Path.of(fileName);
        if (requested.isAbsolute() || requested.getNameCount() != 1 || !requested.getFileName().toString().equals(fileName)) {
            throw new InvalidDiagramPathException("Diagram downloads require a file name without path segments");
        }

        final Path root = Path.of(properties.getOutputDir()).toAbsolutePath().normalize();
        final Path target = root.resolve(requested).normalize();
        if (!target.startsWith(root)) {
            throw new InvalidDiagramPathException("Diagram path escapes configured diagram directory");
        }
        if (!Files.isRegularFile(target)) {
            return null;
        }

        final Path realRoot = root.toRealPath();
        final Path realTarget = target.toRealPath();
        if (!realTarget.startsWith(realRoot)) {
            throw new InvalidDiagramPathException("Diagram path escapes configured diagram directory");
        }
        return realTarget;
    }

}
