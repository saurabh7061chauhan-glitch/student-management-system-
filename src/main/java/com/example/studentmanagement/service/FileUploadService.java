package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.StudyMaterial;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    @Value("${file.upload-dir:uploads}")
    private String uploadDirProperty;

    private Path rootDir;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "ppt", "pptx", "doc", "docx", "txt", "zip", "png", "jpg", "jpeg"
    );

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/zip",
            "application/x-zip-compressed",
            "application/octet-stream",
            "image/png",
            "image/jpeg",
            "image/jpg"
    );

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB

    public static class StoredFileInfo {
        private final String originalFileName;
        private final String storedFileName;
        private final String storagePath;
        private final String fileUrl;
        private final String fileType;
        private final String mimeType;
        private final long fileSize;
        private final String formattedFileSize;

        public StoredFileInfo(String originalFileName, String storedFileName, String storagePath,
                              String fileUrl, String fileType, String mimeType,
                              long fileSize, String formattedFileSize) {
            this.originalFileName = originalFileName;
            this.storedFileName = storedFileName;
            this.storagePath = storagePath;
            this.fileUrl = fileUrl;
            this.fileType = fileType;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
            this.formattedFileSize = formattedFileSize;
        }

        public String getOriginalFileName() { return originalFileName; }
        public String getStoredFileName() { return storedFileName; }
        public String getStoragePath() { return storagePath; }
        public String getFileUrl() { return fileUrl; }
        public String getFileType() { return fileType; }
        public String getMimeType() { return mimeType; }
        public long getFileSize() { return fileSize; }
        public String getFormattedFileSize() { return formattedFileSize; }
    }

    public FileUploadService() {
        // Fallback rootDir if Spring lifecycle hasn't invoked postConstruct yet
        this.rootDir = Paths.get("uploads").toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        this.rootDir = Paths.get(uploadDirProperty != null ? uploadDirProperty : "uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir.resolve("assignments"));
            Files.createDirectories(rootDir.resolve("submissions"));
            Files.createDirectories(rootDir.resolve("materials"));
            Files.createDirectories(rootDir.resolve("avatars"));

            seedSampleMaterialsIfMissing();
            log.info("File upload directories initialized at: {}", rootDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload directories at " + rootDir, e);
        }
    }

    private void seedSampleMaterialsIfMissing() {
        try {
            Path materialsDir = rootDir.resolve("materials");
            Files.createDirectories(materialsDir);

            String dummyPdf = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
                    "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
                    "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\nendobj\n" +
                    "xref\n0 4\n0000000000 65535 f\n0000000010 00000 n\n0000000060 00000 n\n0000000118 00000 n\n" +
                    "trailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n190\n%%EOF\n";

            String[] samples = {"sample_dsa_notes.pdf", "sample_dbms_guide.pdf", "sample_web_handbook.pdf"};
            for (String sample : samples) {
                Path samplePath = materialsDir.resolve(sample);
                if (!Files.exists(samplePath)) {
                    Files.write(samplePath, dummyPdf.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            log.warn("Could not seed sample study material files: {}", e.getMessage());
        }
    }

    public StoredFileInfo storeMaterialFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File attachment is required.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size (" + formatFileSize(file.getSize()) + ") exceeds the 15MB limit.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new IllegalArgumentException("Invalid file format. File must have a valid extension.");
        }

        // Sanitize original filename (remove directory traversal components)
        String cleanOriginalName = Paths.get(originalName).getFileName().toString();
        cleanOriginalName = cleanOriginalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        String ext = cleanOriginalName.substring(cleanOriginalName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("File extension '." + ext + "' is not supported. Allowed: PDF, PPT, PPTX, DOC, DOCX, TXT, ZIP, PNG, JPG.");
        }

        String mimeType = file.getContentType();
        if (mimeType != null && !mimeType.isBlank()) {
            String lowerMime = mimeType.toLowerCase();
            boolean mimeAllowed = ALLOWED_MIME_TYPES.stream().anyMatch(lowerMime::startsWith);
            if (!mimeAllowed) {
                throw new IllegalArgumentException("File MIME type '" + mimeType + "' is not allowed.");
            }
        } else {
            mimeType = determineMimeType(ext);
        }

        try {
            Path materialsDir = rootDir.resolve("materials").normalize();
            Files.createDirectories(materialsDir);

            // Generate unique filename to prevent overwriting existing files
            String storedFileName = UUID.randomUUID().toString() + "_" + cleanOriginalName;
            Path targetLocation = materialsDir.resolve(storedFileName).normalize();

            // Strict Path Traversal Guard
            if (!targetLocation.startsWith(materialsDir)) {
                throw new SecurityException("Potential directory traversal attack detected.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/materials/" + storedFileName;
            String storagePath = targetLocation.toString();
            String fileType = ext.toUpperCase();
            String formattedSize = formatFileSize(file.getSize());

            return new StoredFileInfo(
                    cleanOriginalName,
                    storedFileName,
                    storagePath,
                    fileUrl,
                    fileType,
                    mimeType,
                    file.getSize(),
                    formattedSize
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist file: " + e.getMessage(), e);
        }
    }

    public String storeFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 15MB limit.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new IllegalArgumentException("Invalid file format.");
        }

        String cleanOriginalName = Paths.get(originalName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String ext = cleanOriginalName.substring(cleanOriginalName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("File type '." + ext + "' is not supported.");
        }

        try {
            Path targetDir = rootDir.resolve(subDirectory).normalize();
            Files.createDirectories(targetDir);

            String safeFileName = UUID.randomUUID().toString() + "_" + cleanOriginalName;
            Path targetLocation = targetDir.resolve(safeFileName).normalize();

            if (!targetLocation.startsWith(targetDir)) {
                throw new SecurityException("Potential directory traversal attack detected.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + subDirectory + "/" + safeFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public Resource loadMaterialResource(StudyMaterial material) {
        if (material == null) return null;

        // Try direct storagePath first
        if (material.getStoragePath() != null && !material.getStoragePath().isBlank()) {
            Path path = Paths.get(material.getStoragePath()).normalize();
            if (Files.exists(path) && Files.isReadable(path)) {
                try {
                    return new UrlResource(path.toUri());
                } catch (MalformedURLException ignored) {}
            }
        }

        // Try storedFileName inside materials directory
        if (material.getStoredFileName() != null && !material.getStoredFileName().isBlank()) {
            Path path = rootDir.resolve("materials").resolve(material.getStoredFileName()).normalize();
            if (Files.exists(path) && Files.isReadable(path)) {
                try {
                    return new UrlResource(path.toUri());
                } catch (MalformedURLException ignored) {}
            }
        }

        // Try fileUrl
        if (material.getFileUrl() != null && !material.getFileUrl().isBlank()) {
            String url = material.getFileUrl();
            if (url.startsWith("/uploads/")) {
                url = url.substring(9);
            } else if (url.startsWith("/materials/")) {
                url = "materials/" + url.substring(11);
            } else if (url.startsWith("/")) {
                url = url.substring(1);
            }
            Path path = rootDir.resolve(url).normalize();
            if (Files.exists(path) && Files.isReadable(path)) {
                try {
                    return new UrlResource(path.toUri());
                } catch (MalformedURLException ignored) {}
            }
        }

        return null;
    }

    public Resource loadFileAsResource(String fileUri) {
        try {
            String cleanUri = fileUri.startsWith("/uploads/") ? fileUri.substring(9) : fileUri;
            if (cleanUri.startsWith("/materials/")) {
                cleanUri = "materials/" + cleanUri.substring(11);
            } else if (cleanUri.startsWith("/")) {
                cleanUri = cleanUri.substring(1);
            }
            Path filePath = rootDir.resolve(cleanUri).normalize();
            if (!filePath.startsWith(rootDir)) {
                throw new SecurityException("Unauthorized path traversal attempt.");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + fileUri);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed file URI: " + fileUri, e);
        }
    }

    private String determineMimeType(String ext) {
        return switch (ext.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            case "zip" -> "application/zip";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public Path getRootDir() {
        return rootDir;
    }
}
