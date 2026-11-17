package com.example.multimedia.file_upload_api.repository.email;

import com.example.multimedia.file_upload_api.entity.email.EmailTemplateRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailTemplateRevisionRepository extends JpaRepository<EmailTemplateRevision, Long> {
}
