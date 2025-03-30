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

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwplib.writer.HWPWriter;
import lombok.extern.slf4j.Slf4j;
import pe.yuseok.kim.hwpconvert.model.ConversionTask;
import pe.yuseok.kim.hwpconvert.util.FileUtils;

@Slf4j
@Service
public class ConversionService {

    private final QueueService queueService;
    private final FileUtils fileUtils;
    
    @Value("${conversion.temp-dir:./temp}")
    private String tempDir;
    
    @Value("${conversion.output-dir:./output}")
    private String outputDir;
    
    public ConversionService(@Lazy QueueService queueService, FileUtils fileUtils) {
        this.queueService = queueService;
        this.fileUtils = fileUtils;
    }

    public ConversionTask queueConversion(String userId, MultipartFile file, String targetFormat) throws IOException {
        // Validate file
        String contentType = file.getContentType();
        if (contentType == null || !isSupportedContentType(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
        
        // Create task
        ConversionTask task = ConversionTask.create(
                userId,
                file.getOriginalFilename(),
                contentType,
                targetFormat
        );
        
        // Save uploaded file to temp directory
        String tempFilePath = saveToTempDir(file);
        
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
            
            // Process file based on content type and target format
            String resultFilePath = convertFile(sourceFile, task.getSourceFileContentType(), task.getTargetFormat());
            
            // Update task with result
            task.setStatus("COMPLETED");
            task.setResultFileUrl(resultFilePath);
            queueService.updateTask(task);
            
        } catch (Exception e) {
            log.error("Error processing conversion task: " + task.getId(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            queueService.updateTask(task);
        }
    }
    
    private String convertFile(File sourceFile, String sourceType, String targetFormat) throws Exception {
        String resultPath;
        
        switch (sourceType.toLowerCase()) {
            case "application/haansofthwp":
            case "application/x-hwp":
                resultPath = convertHwpFile(sourceFile, targetFormat);
                break;
            case "application/msword":
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                resultPath = convertDocFile(sourceFile, targetFormat);
                break;
            default:
                throw new IllegalArgumentException("Unsupported source type: " + sourceType);
        }
        
        return resultPath;
    }
    
    private String convertHwpFile(File hwpFile, String targetFormat) throws Exception {
        String outputFileName = UUID.randomUUID().toString() + "." + targetFormat.toLowerCase();
        String outputPath = Paths.get(outputDir, outputFileName).toString();
        
        // Ensure output directory exists
        Files.createDirectories(Paths.get(outputDir));
        
        HWPFile hwpDocument = HWPReader.fromFile(hwpFile);
        
        switch (targetFormat.toLowerCase()) {
            case "txt":
                // Extract text from HWP
                String text = TextExtractor.extract(hwpDocument, TextExtractMethod.InsertControlTextBetweenParagraphText);
                Files.writeString(Paths.get(outputPath), text);
                break;
            case "pdf":
                // For actual implementation, you would need to use a library like PDFBox
                // or other conversion tool. This is a placeholder.
                throw new UnsupportedOperationException("HWP to PDF conversion not implemented yet");
            default:
                throw new IllegalArgumentException("Unsupported target format for HWP: " + targetFormat);
        }
        
        return outputPath;
    }
    
    private String convertDocFile(File docFile, String targetFormat) {
        // Placeholder for DOC/DOCX conversion
        // For actual implementation, you would need to use a library like Apache POI
        throw new UnsupportedOperationException("DOC conversion not implemented yet");
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
               contentType.equals("application/x-hwp") ||
               contentType.equals("application/msword") ||
               contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
} 