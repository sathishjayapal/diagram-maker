package me.sathish.diagram_maker.service;

import java.io.File;
import java.time.Duration;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.sathish.diagram_maker.model.FileData;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;


@Service
@Slf4j
public class FileDataService {

    public static final String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads";

    private String encodeFileName(final String fileName) {
        return fileName.replaceAll("[^0-9a-zA-Z.!\\-_\\[\\]]", "-");
    }

    @SneakyThrows
    public FileData saveUpload(final MultipartFile uploadFile) {
        if (uploadFile.isEmpty()) {
            // no file submitted or no content
            return null;
        }

        log.info("saving uploaded file {}", uploadFile.getOriginalFilename());

        final String uid = UUID.randomUUID().toString();
        final String encodedFileName = encodeFileName(uploadFile.getOriginalFilename());
        final File tempDir = new File(UPLOAD_DIRECTORY + "/" + uid);
        if (!tempDir.mkdirs()) {
            throw new RuntimeException("could not prepare temporary directory " + tempDir.getPath());
        }
        final File tempFile = new File(tempDir, encodedFileName);
        uploadFile.transferTo(tempFile);

        final FileData fileData = new FileData();
        fileData.setUid(uid);
        fileData.setFileName(encodedFileName);
        return fileData;
    }

    public void persistUpload(final FileData fileData) {
        if (fileData == null) {
            return;
        }

        log.info("persisting file upload {}", fileData.getUid());

        final File tempFile = new File(UPLOAD_DIRECTORY + "/" + fileData.getUid() + "/" + fileData.getFileName());

        // TODO add file persistence

        if (!tempFile.delete()) {
            log.error("could not delete file {}", tempFile.getPath());
        }
    }

    public void removeFileContent(final FileData fileData) {
        // TODO add file removal
    }

    public void handleUpdate(final FileData oldFileData, final FileData newFileData) {
        if (oldFileData != null && newFileData != null && oldFileData.getUid().equals(newFileData.getUid())) {
            // no change
            return;
        }
        if (oldFileData != null) {
            removeFileContent(oldFileData);
        }
        if (newFileData != null) {
            persistUpload(newFileData);
        }
    }

    @Scheduled(cron = "0 0 0-23/2 * * *")
    public void cleanUploadDir() {
        log.info("cleaning upload dir");
        final File uploadDir = new File(UPLOAD_DIRECTORY);
        final File[] subDirs = uploadDir.listFiles();
        if (subDirs == null) {
            return;
        }
        final long cutoff = System.currentTimeMillis() - Duration.ofHours(1).toMillis();
        for (final File subDir : subDirs) {
            if (subDir.lastModified() < cutoff) {
                if (!FileSystemUtils.deleteRecursively(subDir)) {
                    log.error("could not delete directory {}", subDir.getPath());
                }
            }
        }
    }

}
