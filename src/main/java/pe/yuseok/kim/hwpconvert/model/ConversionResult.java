package pe.yuseok.kim.hwpconvert.model;

import java.time.LocalDateTime;

public class ConversionResult {
    private String sourceFileName;
    private String convertedFileName;
    private String sourceFormat;
    private String targetFormat;
    private boolean success;
    private String errorMessage;
    private LocalDateTime completionTime;
    private String downloadUrl;

    public ConversionResult() {
        this.completionTime = LocalDateTime.now();
    }

    public ConversionResult(String sourceFileName, String convertedFileName, String sourceFormat, 
                          String targetFormat, boolean success) {
        this.sourceFileName = sourceFileName;
        this.convertedFileName = convertedFileName;
        this.sourceFormat = sourceFormat;
        this.targetFormat = targetFormat;
        this.success = success;
        this.completionTime = LocalDateTime.now();
    }

    // Getters and setters
    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getConvertedFileName() {
        return convertedFileName;
    }

    public void setConvertedFileName(String convertedFileName) {
        this.convertedFileName = convertedFileName;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public void setSourceFormat(String sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.success = false;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
} 