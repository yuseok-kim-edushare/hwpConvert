package pe.yuseok.kim.hwpconvert.service.conversion;

import pe.yuseok.kim.hwpconvert.model.ConversionResult;
import java.io.File;

/**
 * Interface for defining different document conversion strategies
 */
public interface ConversionStrategy {
    
    /**
     * Converts a document from one format to another
     * 
     * @param inputFile The source file to convert
     * @param outputDir The directory where the converted file will be saved
     * @param targetFormat The format to convert to
     * @return A ConversionResult object containing the result of the conversion
     */
    ConversionResult convert(File inputFile, File outputDir, String targetFormat);
    
    /**
     * Returns true if this strategy can handle the given source format
     * 
     * @param sourceFormat The source format to check
     * @return true if this strategy can handle the given source format
     */
    boolean supportsSourceFormat(String sourceFormat);
    
    /**
     * Returns true if this strategy can convert to the given target format
     * 
     * @param targetFormat The target format to check
     * @return true if this strategy can convert to the given target format
     */
    boolean supportsTargetFormat(String targetFormat);
} 