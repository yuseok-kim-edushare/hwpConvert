package pe.yuseok.kim.hwpconvert.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.yuseok.kim.hwpconvert.model.entity.Document;
import pe.yuseok.kim.hwpconvert.model.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByOwner(User owner);
    
    List<Document> findByOwnerOrderByUploadDateDesc(User owner);
    
    Optional<Document> findByDownloadToken(String downloadToken);
    
    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND d.converted = true ORDER BY d.conversionDate DESC")
    List<Document> findConvertedDocumentsByOwner(@Param("owner") User owner);
    
    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND d.converted = false AND d.conversionError IS NULL ORDER BY d.uploadDate DESC")
    List<Document> findPendingDocumentsByOwner(@Param("owner") User owner);
    
    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND d.conversionError IS NOT NULL ORDER BY d.uploadDate DESC")
    List<Document> findFailedDocumentsByOwner(@Param("owner") User owner);
    
    @Query("SELECT d FROM Document d WHERE d.uploadDate < :date")
    List<Document> findOlderThan(@Param("date") LocalDateTime date);
} 