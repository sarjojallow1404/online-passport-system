
package com.passport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.passport.entity.ApplicationDocument;

public interface ApplicationDocumentRepository
        extends JpaRepository<ApplicationDocument, Long> {

    List<ApplicationDocument>
        findAllByApplicationIdOrderByUploadedAtDesc(Long applicationId);

    Optional<ApplicationDocument>
        findFirstByApplicationIdAndDocumentTypeOrderByUploadedAtDesc(
                Long applicationId,
                String documentType);
}
