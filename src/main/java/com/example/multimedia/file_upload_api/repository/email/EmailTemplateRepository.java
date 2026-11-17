package com.example.multimedia.file_upload_api.repository.email;

import com.example.multimedia.file_upload_api.entity.email.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    Optional<EmailTemplate> findByTemplateKeyAndIsActiveTrue(String templateKey);
}
