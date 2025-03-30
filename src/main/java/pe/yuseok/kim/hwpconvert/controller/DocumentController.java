package pe.yuseok.kim.hwpconvert.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import pe.yuseok.kim.hwpconvert.model.entity.Document;
import pe.yuseok.kim.hwpconvert.model.entity.User;
import pe.yuseok.kim.hwpconvert.service.DocumentService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/documents")
public class DocumentController {
    
    private final DocumentService documentService;
    
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    @GetMapping
    public String listDocuments(@AuthenticationPrincipal User user, Model model) {
        List<Document> convertedDocuments = documentService.getConvertedDocumentsByUser(user);
        List<Document> pendingDocuments = documentService.getPendingDocumentsByUser(user);
        List<Document> failedDocuments = documentService.getFailedDocumentsByUser(user);
        
        model.addAttribute("convertedDocuments", convertedDocuments);
        model.addAttribute("pendingDocuments", pendingDocuments);
        model.addAttribute("failedDocuments", failedDocuments);
        
        return "documents/list";
    }
    
    @GetMapping("/{id}")
    public String viewDocument(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        Document document = documentService.getDocumentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        
        // Check if the user is the owner of the document
        if (!document.getOwner().getId().equals(user.getId())) {
            return "redirect:/documents?error=unauthorized";
        }
        
        model.addAttribute("document", document);
        return "documents/view";
    }
    
    @GetMapping("/download/{token}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String token) {
        Document document = documentService.getDocumentByDownloadToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid download token"));
        
        if (!document.isConverted() || document.getConvertedFilename() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Resource resource = documentService.loadFileAsResource(document.getConvertedFilename());
            
            // Encode the filename to ensure it's properly handled in HTTP headers
            String encodedFilename = URLEncoder.encode(document.getOriginalFilename(), StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
            
            // Set content-disposition header to trigger download
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename*=UTF-8''" + encodedFilename + "." + document.getConvertedFormat())
                    .body(resource);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{id}/delete")
    public String deleteDocument(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Document document = documentService.getDocumentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        
        // Check if the user is the owner of the document
        if (!document.getOwner().getId().equals(user.getId())) {
            return "redirect:/documents?error=unauthorized";
        }
        
        documentService.deleteDocument(document);
        return "redirect:/documents?success=deleted";
    }
} 