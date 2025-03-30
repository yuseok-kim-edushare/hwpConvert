package pe.yuseok.kim.hwpconvert.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileUtils {

    @Value("${conversion.temp-dir:./temp}")
    private String tempDir;
    
    @Value("${conversion.output-dir:./output}")
    private String outputDir;
    
    /**
     * Creates necessary directories for file operations
     */
    public void createDirectories() {
        try {
            Files.createDirectories(Paths.get(tempDir));
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            log.error("Failed to create directories", e);
            throw new RuntimeException("Failed to create directories", e);
        }
    }
    
    /**
     * Deletes files older than a specified number of days
     * @param days Number of days
     * @param directory Directory to clean up
     */
    public void cleanupOldFiles(int days, String directory) {
        Path dirPath = Paths.get(directory);
        if (!Files.exists(dirPath)) {
            return;
        }
        
        try {
            Files.list(dirPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < 
                               System.currentTimeMillis() - days * 86400000L;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.info("Deleted old file: {}", path);
                    } catch (IOException e) {
                        log.warn("Failed to delete file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Error during file cleanup", e);
        }
    }
    
    /**
     * Creates a temporary file from a MultipartFile
     * @param file The uploaded file
     * @return The path to the temporary file
     */
    public Path createTempFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String tempFilename = UUID.randomUUID().toString() + extension;
        Path tempFilePath = Paths.get(tempDir, tempFilename);
        
        Files.createDirectories(Paths.get(tempDir));
        Files.write(tempFilePath, file.getBytes());
        
        return tempFilePath;
    }
    
    /**
     * Creates an output file path with a unique name
     * @param originalName Original file name
     * @param extension The extension for the output file
     * @return The path to the output file
     */
    public Path createOutputFilePath(String originalName, String extension) throws IOException {
        Files.createDirectories(Paths.get(outputDir));
        
        String baseName = originalName;
        if (baseName.contains(".")) {
            baseName = baseName.substring(0, baseName.lastIndexOf("."));
        }
        
        // Sanitize filename
        baseName = baseName.replaceAll("[^a-zA-Z0-9-_]", "_");
        
        String outputFilename = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        return Paths.get(outputDir, outputFilename);
    }
} 