package pe.yuseok.kim.hwpconvert.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import pe.yuseok.kim.hwpconvert.model.entity.Document;
import pe.yuseok.kim.hwpconvert.model.entity.User;
import pe.yuseok.kim.hwpconvert.repository.DocumentRepository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    
    @Value("${conversion.output-dir:./output}")
    private String outputDir;
    
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }
    
    public List<Document> getDocumentsByUser(User user) {
        return documentRepository.findByOwnerOrderByUploadDateDesc(user);
    }
    
    public List<Document> getConvertedDocumentsByUser(User user) {
        return documentRepository.findConvertedDocumentsByOwner(user);
    }
    
    public List<Document> getPendingDocumentsByUser(User user) {
        return documentRepository.findPendingDocumentsByOwner(user);
    }
    
    public List<Document> getFailedDocumentsByUser(User user) {
        return documentRepository.findFailedDocumentsByOwner(user);
    }
    
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }
    
    public Optional<Document> getDocumentByDownloadToken(String token) {
        return documentRepository.findByDownloadToken(token);
    }
    
    public Resource loadFileAsResource(String filename) throws MalformedURLException {
        Path filePath = Paths.get(outputDir).resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists()) {
            return resource;
        } else {
            throw new RuntimeException("File not found: " + filename);
        }
    }
    
    public void deleteDocument(Document document) {
        // Delete the converted file if it exists
        if (document.isConverted() && document.getConvertedFilename() != null) {
            try {
                Path filePath = Paths.get(outputDir, document.getConvertedFilename());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.error("Error deleting converted file: {}", document.getConvertedFilename(), e);
            }
        }
        
        // Delete the original file if it exists
        try {
            Path filePath = Paths.get(outputDir, document.getStoredFilename());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Error deleting original file: {}", document.getStoredFilename(), e);
        }
        
        // Delete the document from the database
        documentRepository.delete(document);
    }
    
    public void cleanupOldDocuments(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        List<Document> oldDocuments = documentRepository.findOlderThan(cutoffDate);
        
        log.info("Found {} documents older than {} days for cleanup", oldDocuments.size(), days);
        
        for (Document document : oldDocuments) {
            deleteDocument(document);
        }
    }
} 