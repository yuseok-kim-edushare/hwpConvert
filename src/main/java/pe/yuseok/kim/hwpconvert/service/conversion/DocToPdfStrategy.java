package pe.yuseok.kim.hwpconvert.service.conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.yuseok.kim.hwpconvert.model.ConversionResult;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Strategy for converting DOC/DOCX files to PDF format
 */
@Component
public class DocToPdfStrategy implements ConversionStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(DocToPdfStrategy.class);
    private static final List<String> SUPPORTED_SOURCE_FORMATS = Arrays.asList("doc", "docx");
    private static final List<String> SUPPORTED_TARGET_FORMATS = Arrays.asList("pdf");
    
    @Override
    public ConversionResult convert(File inputFile, File outputDir, String targetFormat) {
        logger.info("Converting {} to {} format", inputFile.getName(), targetFormat);
        
        // Create result object
        ConversionResult result = new ConversionResult();
        result.setSourceFileName(inputFile.getName());
        
        // Determine source format from file extension
        String sourceFormat = inputFile.getName().substring(inputFile.getName().lastIndexOf(".") + 1).toLowerCase();
        result.setSourceFormat(sourceFormat);
        result.setTargetFormat(targetFormat);
        
        try {
            // Here we would integrate with a Word document conversion library
            // For example, we could use Apache POI or another library
            
            // This would be replaced with actual conversion code
            // For example using Apache POI:
            // XWPFDocument document = new XWPFDocument(new FileInputStream(inputFile));
            // PdfOptions options = PdfOptions.create();
            // PdfConverter.getInstance().convert(document, new FileOutputStream(outputFile), options);
            
            // For now, simulate conversion by creating an output file path
            String outputFileName = inputFile.getName().replaceFirst("[.][^.]+$", "") + "." + targetFormat;
            Path outputPath = Paths.get(outputDir.getAbsolutePath(), outputFileName);
            
            // Log the conversion process
            logger.info("Converted file would be saved at: {}", outputPath);
            
            // In a real implementation, we would check if the conversion succeeded
            // For demonstration, we'll assume success
            result.setConvertedFileName(outputFileName);
            result.setSuccess(true);
            
            // For demonstration, set a download URL
            result.setDownloadUrl("/download/" + outputFileName);
            
        } catch (Exception e) {
            logger.error("Error converting DOC/DOCX file to PDF", e);
            result.setSuccess(false);
            result.setErrorMessage("Failed to convert DOC/DOCX to PDF: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public boolean supportsSourceFormat(String sourceFormat) {
        return SUPPORTED_SOURCE_FORMATS.contains(sourceFormat.toLowerCase());
    }
    
    @Override
    public boolean supportsTargetFormat(String targetFormat) {
        return SUPPORTED_TARGET_FORMATS.contains(targetFormat.toLowerCase());
    }
} 