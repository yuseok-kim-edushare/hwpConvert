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
 * Strategy for converting HWP files to PDF format
 */
@Component
public class HwpToPdfStrategy implements ConversionStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(HwpToPdfStrategy.class);
    private static final List<String> SUPPORTED_SOURCE_FORMATS = Arrays.asList("hwp");
    private static final List<String> SUPPORTED_TARGET_FORMATS = Arrays.asList("pdf");
    
    @Override
    public ConversionResult convert(File inputFile, File outputDir, String targetFormat) {
        logger.info("Converting {} to {} format", inputFile.getName(), targetFormat);
        
        // Create result object
        ConversionResult result = new ConversionResult();
        result.setSourceFileName(inputFile.getName());
        result.setSourceFormat("hwp");
        result.setTargetFormat(targetFormat);
        
        try {
            // Here we would integrate with HWP conversion library
            // For now, this is a placeholder for the actual conversion logic
            
            // This would be replaced with actual conversion code
            // For example: HwpDocInfo hwpDoc = HwpReader.fromFile(inputFile);
            //              hwpDoc.convertToPdf(outputFile);
            
            // In a real implementation, we would need to include the HWP library dependency
            // and use the actual conversion APIs
            
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
            logger.error("Error converting HWP file to PDF", e);
            result.setSuccess(false);
            result.setErrorMessage("Failed to convert HWP to PDF: " + e.getMessage());
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