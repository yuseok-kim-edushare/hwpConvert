package pe.yuseok.kim.hwpconvert.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String originalFilename;
    
    @Column(nullable = false)
    private String storedFilename;
    
    @Column(nullable = false)
    private String originalFormat;
    
    @Column
    private String convertedFilename;
    
    @Column
    private String convertedFormat;
    
    @Column(nullable = false)
    private LocalDateTime uploadDate;
    
    @Column
    private LocalDateTime conversionDate;
    
    @Column(nullable = false)
    private boolean converted = false;
    
    @Column
    private String conversionError;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;
    
    @Column(nullable = false)
    private String downloadToken;

    // Constructors
    public Document() {
        this.uploadDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getOriginalFormat() {
        return originalFormat;
    }

    public void setOriginalFormat(String originalFormat) {
        this.originalFormat = originalFormat;
    }

    public String getConvertedFilename() {
        return convertedFilename;
    }

    public void setConvertedFilename(String convertedFilename) {
        this.convertedFilename = convertedFilename;
    }

    public String getConvertedFormat() {
        return convertedFormat;
    }

    public void setConvertedFormat(String convertedFormat) {
        this.convertedFormat = convertedFormat;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public LocalDateTime getConversionDate() {
        return conversionDate;
    }

    public void setConversionDate(LocalDateTime conversionDate) {
        this.conversionDate = conversionDate;
    }

    public boolean isConverted() {
        return converted;
    }

    public void setConverted(boolean converted) {
        this.converted = converted;
    }

    public String getConversionError() {
        return conversionError;
    }

    public void setConversionError(String conversionError) {
        this.conversionError = conversionError;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getDownloadToken() {
        return downloadToken;
    }

    public void setDownloadToken(String downloadToken) {
        this.downloadToken = downloadToken;
    }
} 