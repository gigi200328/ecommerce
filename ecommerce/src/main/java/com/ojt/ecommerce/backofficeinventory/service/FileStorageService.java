package com.ojt.ecommerce.backofficeinventory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ojt.ecommerce.enums.UploadType;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Map<String, String> EXTENSIONS = Map.of(
            "JPEG", ".jpg",
            "PNG", ".png",
            "GIF", ".gif"
    );

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String store(MultipartFile file, UploadType type) {

        validate(file);

        Path target = null;

        try {
            String extension = detectExtension(file);

            Path directory = getDirectory(type);
            Files.createDirectories(directory);

            String filename = UUID.randomUUID() + extension;
            target = directory.resolve(filename);

            try (var input = file.getInputStream()) {
                Files.copy(input, target);
            }

            return "/uploads/"
                    + type.getDirectoryName()
                    + "/"
                    + filename;

        } catch (IOException ex) {

            if (target != null) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException cleanupEx) {
                    ex.addSuppressed(cleanupEx);
                }
            }

            throw new RuntimeException(
                    "Could not store " + type.name(), ex
            );
        }
    }

    public void delete(String fileUrl, UploadType type) {

        String prefix =
                "/uploads/" + type.getDirectoryName() + "/";

        // External URLs and files belonging to other folders
        // must not be deleted.
        if (fileUrl == null || !fileUrl.startsWith(prefix)) {
            return;
        }

        String filename = fileUrl.substring(prefix.length());

        if (!filename.matches(
                "[0-9a-fA-F-]{36}\\.(jpg|png|gif)")) {
            throw new IllegalArgumentException(
                    "Invalid stored filename"
            );
        }

        Path directory = getDirectory(type);
        Path target = directory.resolve(filename).normalize();

        if (!target.getParent().equals(directory)) {
            throw new IllegalArgumentException(
                    "Invalid file path"
            );
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Could not delete file: " + fileUrl, ex
            );
        }
    }

    private void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Image file is required"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image must not exceed 5MB"
            );
        }

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(java.util.Locale.ROOT) : "";

        if (!java.util.Set.of(
                "image/jpeg",
                "image/jpg",
                "image/pjpeg",
                "image/png",
                "image/gif",
                "image/webp"
        ).contains(contentType)) {
            throw new IllegalArgumentException(
                    "Only JPG, PNG and GIF are allowed"
            );
        }
    }

    private String detectExtension(MultipartFile file)
            throws IOException {

        try (var input = file.getInputStream();
             ImageInputStream imageInput =
                     ImageIO.createImageInputStream(input)) {

            if (imageInput == null) {
                throw new IllegalArgumentException(
                        "Invalid image file"
                );
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageInput);

            if (!readers.hasNext()) {
                String origName = file.getOriginalFilename();
                if (origName != null && origName.contains(".")) {
                    String ext = origName.substring(origName.lastIndexOf(".")).toLowerCase(java.util.Locale.ROOT);
                    if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".gif") || ext.equals(".webp")) {
                        return ext.equals(".jpeg") ? ".jpg" : ext;
                    }
                }
                throw new IllegalArgumentException(
                        "Invalid image content"
                );
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInput);

                String format =
                        reader.getFormatName().toUpperCase(java.util.Locale.ROOT);

                String extension = EXTENSIONS.get(format);

                if (extension == null) {
                    if ("JPEG".equals(format) || "JPG".equals(format)) {
                        extension = ".jpg";
                    } else {
                        extension = "." + format.toLowerCase(java.util.Locale.ROOT);
                    }
                }

                // Force the decoder to read the image.
                reader.read(0);

                return extension;

            } finally {
                reader.dispose();
            }
        }
    }

    private Path getDirectory(UploadType type) {
        return Paths.get(uploadDir)
                .resolve(type.getDirectoryName())
                .toAbsolutePath()
                .normalize();
    }
}