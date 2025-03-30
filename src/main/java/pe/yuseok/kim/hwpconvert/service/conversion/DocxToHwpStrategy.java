package pe.yuseok.kim.hwpconvert.service.conversion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.yuseok.kim.hwpconvert.model.ConversionResult;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Strategy for converting DOCX files to HWP format
 */
@Slf4j
@Component
public class DocxToHwpStrategy implements ConversionStrategy {

    private static final List<String> SUPPORTED_SOURCE_FORMATS = Arrays.asList("docx");
    private static final List<String> SUPPORTED_TARGET_FORMATS = Arrays.asList("hwp");

    @Override
    public boolean supportsSourceFormat(String sourceFormat) {
        return SUPPORTED_SOURCE_FORMATS.contains(sourceFormat.toLowerCase());
    }

    @Override
    public boolean supportsTargetFormat(String targetFormat) {
        return SUPPORTED_TARGET_FORMATS.contains(targetFormat.toLowerCase());
    }

    @Override
    public ConversionResult convert(File sourceFile, File outputDirectory, String targetFormat) {
        log.info("Converting DOCX file to HWP: {}", sourceFile.getName());
        
        try {
            // Generate output file name
            String baseName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.'));
            String outputFileName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + targetFormat;
            Path outputPath = Paths.get(outputDirectory.getAbsolutePath(), outputFileName);
            
            // TODO: Implement the actual conversion logic using libraries like POI-OOXML for DOCX and hwplib for HWP
            // This is a placeholder for demonstration purposes
            
            // For now, simulate conversion with a delay to mimic processing time
            Thread.sleep(2000);
            
            // In a real implementation, you would:
            // 1. Parse the DOCX file using Apache POI OOXML
            // 2. Create a new HWP file using hwplib
            // 3. Convert content from DOCX model to HWP model
            // 4. Save the new HWP file to the output path
            
            // Create a stub empty file for demonstration
            boolean stubFileCreated = outputPath.toFile().createNewFile();
            if (!stubFileCreated) {
                throw new RuntimeException("Failed to create output file: " + outputPath);
            }
            
            ConversionResult result = new ConversionResult();
            result.setSourceFileName(sourceFile.getName());
            result.setConvertedFileName(outputFileName);
            result.setSourceFormat("docx");
            result.setTargetFormat(targetFormat);
            result.setSuccess(true);
            result.setCompletionTime(LocalDateTime.now());
            result.setDownloadUrl(outputPath.toString());
            return result;
            
        } catch (Exception e) {
            log.error("Error converting DOCX to HWP", e);
            ConversionResult result = new ConversionResult();
            result.setSuccess(false);
            result.setErrorMessage("Error converting DOCX to HWP: " + e.getMessage());
            return result;
        }
    }
} 