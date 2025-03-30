package pe.yuseok.kim.hwpconvert.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String userId;
    private String sourceFileName;
    private String sourceFileContentType;
    private String targetFormat;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String resultFileUrl;
    private String errorMessage;

    @Builder.Default
    private int priority = 0;

    public static ConversionTask create(String userId, String sourceFileName, String sourceFileContentType, String targetFormat) {
        return ConversionTask.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .sourceFileName(sourceFileName)
                .sourceFileContentType(sourceFileContentType)
                .targetFormat(targetFormat)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }
} 