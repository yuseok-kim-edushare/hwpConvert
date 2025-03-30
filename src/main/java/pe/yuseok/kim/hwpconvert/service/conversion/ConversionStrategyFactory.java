package pe.yuseok.kim.hwpconvert.service.conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Factory for selecting appropriate conversion strategy based on source and target formats
 */
@Component
public class ConversionStrategyFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversionStrategyFactory.class);
    
    private final List<ConversionStrategy> conversionStrategies;
    
    @Autowired
    public ConversionStrategyFactory(List<ConversionStrategy> conversionStrategies) {
        this.conversionStrategies = conversionStrategies;
    }
    
    /**
     * Get the appropriate conversion strategy for the given source and target formats
     * 
     * @param sourceFormat The format of the source file
     * @param targetFormat The desired output format
     * @return An Optional containing the appropriate ConversionStrategy if found, or empty if no suitable strategy exists
     */
    public Optional<ConversionStrategy> getStrategy(String sourceFormat, String targetFormat) {
        logger.info("Finding conversion strategy for {} to {} conversion", sourceFormat, targetFormat);
        
        return conversionStrategies.stream()
                .filter(strategy -> strategy.supportsSourceFormat(sourceFormat) && 
                                    strategy.supportsTargetFormat(targetFormat))
                .findFirst();
    }
    
    /**
     * Check if a conversion from source format to target format is supported
     * 
     * @param sourceFormat The format of the source file
     * @param targetFormat The desired output format
     * @return true if the conversion is supported, false otherwise
     */
    public boolean isConversionSupported(String sourceFormat, String targetFormat) {
        return getStrategy(sourceFormat, targetFormat).isPresent();
    }
    
    /**
     * Get all available conversion strategies
     * 
     * @return List of all conversion strategies
     */
    public List<ConversionStrategy> getAllStrategies() {
        return conversionStrategies;
    }
} 