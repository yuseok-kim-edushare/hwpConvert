package pe.yuseok.kim.hwpconvert.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.yuseok.kim.hwpconvert.model.ConversionTask;
import pe.yuseok.kim.hwpconvert.service.ConversionService;

@Slf4j
@Controller
@RequestMapping("/convert")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;
    
    @GetMapping
    public String convertPage() {
        return "convert/index";
    }
    
    @PostMapping
    public String convertDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat,
            Principal principal,
            Model model) {
        
        try {
            String userId = principal.getName();
            ConversionTask task = conversionService.queueConversion(userId, file, targetFormat);
            
            model.addAttribute("taskId", task.getId());
            return "convert/status";
        } catch (Exception e) {
            log.error("Error during conversion", e);
            model.addAttribute("error", e.getMessage());
            return "convert/index";
        }
    }
    
    @GetMapping("/status/{taskId}")
    @ResponseBody
    public ConversionTask checkStatus(@PathVariable String taskId) {
        return conversionService.getTaskStatus(taskId);
    }
    
    @GetMapping("/download/{taskId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String taskId) throws IOException {
        ConversionTask task = conversionService.getTaskStatus(taskId);
        
        if (task == null || !"COMPLETED".equals(task.getStatus())) {
            return ResponseEntity.notFound().build();
        }
        
        Path filePath = Paths.get(task.getResultFileUrl());
        Resource resource = new UrlResource(filePath.toUri());
        
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        String originalFilename = task.getSourceFileName();
        String extension = task.getTargetFormat().toLowerCase();
        String outputFilename = originalFilename.substring(0, originalFilename.lastIndexOf('.')) + "." + extension;
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFilename + "\"")
                .body(resource);
    }
} 