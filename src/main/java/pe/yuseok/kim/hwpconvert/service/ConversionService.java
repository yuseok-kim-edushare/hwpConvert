package pe.yuseok.kim.hwpconvert.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import pe.yuseok.kim.hwpconvert.model.ConversionResult;
import pe.yuseok.kim.hwpconvert.model.ConversionTask;
import pe.yuseok.kim.hwpconvert.model.entity.Document;
import pe.yuseok.kim.hwpconvert.model.entity.User;
import pe.yuseok.kim.hwpconvert.repository.DocumentRepository;
import pe.yuseok.kim.hwpconvert.repository.UserRepository;
import pe.yuseok.kim.hwpconvert.service.conversion.ConversionStrategyFactory;
import pe.yuseok.kim.hwpconvert.service.conversion.ConversionStrategy;
import pe.yuseok.kim.hwpconvert.util.FileUtils;

@Slf4j
@Service
public class ConversionService {

    private final QueueService queueService;
    private final FileUtils fileUtils;
    private final ConversionStrategyFactory conversionStrategyFactory;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    
    @Value("${conversion.temp-dir:./temp}")
    private String tempDir;
    
    @Value("${conversion.output-dir:./output}")
    private String outputDir;
    
    public ConversionService(
            @Lazy QueueService queueService, 
            FileUtils fileUtils,
            ConversionStrategyFactory conversionStrategyFactory,
            DocumentRepository documentRepository,
            UserRepository userRepository) {
        this.queueService = queueService;
        this.fileUtils = fileUtils;
        this.conversionStrategyFactory = conversionStrategyFactory;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    public ConversionTask queueConversion(String username, MultipartFile file, String targetFormat) throws IOException {
        // Validate file
        String contentType = file.getContentType();
        if (contentType == null || !isSupportedContentType(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
        
        // Get source format from content type
        String sourceFormat = getFormatFromContentType(contentType);
        
        // Check if conversion is supported
        if (!conversionStrategyFactory.isConversionSupported(sourceFormat, targetFormat)) {
            throw new IllegalArgumentException(
                    "Conversion from " + sourceFormat + " to " + targetFormat + " is not supported.");
        }
        
        // Create task
        ConversionTask task = ConversionTask.create(
                username,
                file.getOriginalFilename(),
                contentType,
                targetFormat
        );
        
        // Save uploaded file to temp directory
        String tempFilePath = saveToTempDir(file);
        
        // Create document entity and save to database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        
        Document document = new Document();
        document.setOriginalFilename(file.getOriginalFilename());
        document.setStoredFilename(Paths.get(tempFilePath).getFileName().toString());
        document.setOriginalFormat(sourceFormat);
        document.setConvertedFormat(targetFormat);
        document.setFileSize(file.getSize());
        document.setOwner(user);
        document.setDownloadToken(UUID.randomUUID().toString());
        
        documentRepository.save(document);
        
        // Store file path in Redis
        queueService.storeFilePath(task.getId(), tempFilePath);
        
        // Add task to queue
        queueService.enqueueTask(task);
        
        return task;
    }
    
    public ConversionTask getTaskStatus(String taskId) {
        return queueService.getTask(taskId);
    }
    
    public void processTask(ConversionTask task) {
        try {
            String tempFilePath = queueService.getFilePath(task.getId());
            File sourceFile = new File(tempFilePath);
            
            // Update task status
            task.setStatus("PROCESSING");
            task.setProcessedAt(LocalDateTime.now());
            queueService.updateTask(task);
            
            // Get source format from content type
            String sourceFormat = getFormatFromContentType(task.getSourceFileContentType());
            String targetFormat = task.getTargetFormat();
            
            // Create output directory if it doesn't exist
            File outputDirectory = new File(outputDir);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }
            
            // Process file using strategy pattern
            ConversionResult result = convertFile(sourceFile, sourceFormat, targetFormat);
            
            // Update document in database if conversion succeeded
            updateDocumentAfterConversion(sourceFile.getName(), result);
            
            // Update task with result
            task.setStatus(result.isSuccess() ? "COMPLETED" : "FAILED");
            if (!result.isSuccess()) {
                task.setErrorMessage(result.getErrorMessage());
            } else {
                task.setResultFileUrl(result.getDownloadUrl());
            }
            queueService.updateTask(task);
            
        } catch (Exception e) {
            log.error("Error processing conversion task: " + task.getId(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            queueService.updateTask(task);
        }
    }
    
    private ConversionResult convertFile(File sourceFile, String sourceFormat, String targetFormat) {
        // Get appropriate conversion strategy
        ConversionStrategy strategy = conversionStrategyFactory.getStrategy(sourceFormat, targetFormat)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No conversion strategy found for " + sourceFormat + " to " + targetFormat));
        
        // Create output directory
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        
        // Perform conversion
        return strategy.convert(sourceFile, outputDirectory, targetFormat);
    }
    
    private void updateDocumentAfterConversion(String storedFilename, ConversionResult result) {
        // Find document by stored filename
        Document document = documentRepository.findAll().stream()
                .filter(doc -> doc.getStoredFilename().contains(storedFilename))
                .findFirst()
                .orElse(null);
        
        if (document != null) {
            if (result.isSuccess()) {
                document.setConvertedFilename(result.getConvertedFileName());
                document.setConverted(true);
                document.setConversionDate(result.getCompletionTime());
            } else {
                document.setConversionError(result.getErrorMessage());
            }
            documentRepository.save(document);
        } else {
            log.warn("Document not found for stored filename: {}", storedFilename);
        }
    }
    
    private String saveToTempDir(MultipartFile file) throws IOException {
        // Create temp directory if it doesn't exist
        Path tempDirPath = Paths.get(tempDir);
        Files.createDirectories(tempDirPath);
        
        // Generate a unique file name
        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        Path filePath = tempDirPath.resolve(fileName);
        
        // Save file
        try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            outputStream.write(file.getBytes());
        }
        
        return filePath.toString();
    }
    
    private boolean isSupportedContentType(String contentType) {
        return contentType.equals("application/haansofthwp") ||
               contentType.equals("application/haansofthwpx") ||
               contentType.equals("application/x-hwp") ||
               contentType.equals("application/vnd.hancom.hwpx") ||
               contentType.equals("application/msword") ||
               contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
    
    private String getFormatFromContentType(String contentType) {
        switch (contentType.toLowerCase()) {
            case "application/haansofthwp":
                return "hwp";
            case "application/haansofthwpx":
                return "hwpx";
            case "application/x-hwp":
                return "hwp";
            case "application/vnd.hancom.hwpx":
                return "hwpx";
            case "application/msword":
                return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return "docx";
            default:
                throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
    }
} 