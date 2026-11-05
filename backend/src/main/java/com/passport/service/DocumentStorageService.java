
package com.passport.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
@Service
public class DocumentStorageService {

    private final Path storageDirectory;

    public DocumentStorageService(
            @Value("${passport.documents.storage-dir:${user.home}/.passport-system/private-documents}")
            String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory)
                .toAbsolutePath()
                .normalize();
    }

    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file to upload");
        }

        if (file.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "File size must not exceed 5 MB");
        }

        byte[] signature = new byte[8];

        try (InputStream input = file.getInputStream()) {
            int bytesRead = input.read(signature);

            if (bytesRead < 3) {
                throw new IllegalArgumentException(
                        "The uploaded file is empty or invalid");
            }
        }

        String contentType;
        String extension;

        if (signature[0] == '%' && signature[1] == 'P'
                && signature[2] == 'D' && signature[3] == 'F'
                && signature[4] == '-') {
            contentType = "application/pdf";
            extension = ".pdf";
        } else if ((signature[0] & 0xFF) == 0xFF
                && (signature[1] & 0xFF) == 0xD8
                && (signature[2] & 0xFF) == 0xFF) {
            contentType = "image/jpeg";
            extension = ".jpg";
        } else if (signature.length >= 8
                && (signature[0] & 0xFF) == 0x89
                && signature[1] == 'P'
                && signature[2] == 'N'
                && signature[3] == 'G'
                && (signature[4] & 0xFF) == 0x0D
                && (signature[5] & 0xFF) == 0x0A
                && (signature[6] & 0xFF) == 0x1A
                && (signature[7] & 0xFF) == 0x0A) {
            contentType = "image/png";
            extension = ".png";
        } else {
            throw new IllegalArgumentException(
                    "Unsupported file. Upload a PDF, JPEG, or PNG file");
        }

        Files.createDirectories(storageDirectory);

        String storageKey = UUID.randomUUID() + extension;
        Path destination = storageDirectory.resolve(storageKey).normalize();

        if (!destination.startsWith(storageDirectory)) {
            throw new IOException("Invalid storage destination");
        }

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "uploaded-document" + extension;
        } else {
            // Handle both Unix and Windows path separators.
            originalFilename = originalFilename.replace('\\', '/');
            originalFilename = originalFilename.substring(
                    originalFilename.lastIndexOf('/') + 1);

            if (originalFilename.length() > 255) {
                originalFilename = originalFilename.substring(
                        originalFilename.length() - 255);
            }
        }

        return new StoredFile(
                storageKey,
                originalFilename,
                contentType,
                file.getSize());
    }

    public record StoredFile(
            String storageKey,
            String originalFilename,
            String contentType,
            long fileSizeBytes) {
    }
public Resource load(String storageKey) throws IOException {
    if (storageKey == null
            || !storageKey.matches(
                "[0-9a-fA-F-]{36}\\.(pdf|jpg|png)")) {
        throw new IOException("Invalid document storage key");
    }

    Path file = storageDirectory
            .resolve(storageKey)
            .normalize();

    if (!file.startsWith(storageDirectory)
            || !Files.isRegularFile(file)
            || !Files.isReadable(file)) {
        throw new IOException("Document file not found");
    }

    Resource resource = new UrlResource(file.toUri());

    if (!resource.exists() || !resource.isReadable()) {
        throw new IOException("Document file is unavailable");
    }

    return resource;
}
}
