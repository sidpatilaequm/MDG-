package com.example.multimedia.file_upload_api.entity.email;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Audit snapshot of an {@link EmailTemplate}'s content immediately before an edit. */
@Entity
@Table(name = "email_template_revision")
@Getter
@Setter
public class EmailTemplateRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private EmailTemplate template;

    @Column(nullable = false)
    private String subject;

    @Column(name = "html_body", columnDefinition = "LONGTEXT", nullable = false)
    private String htmlBody;

    @Column(name = "plain_text_body", columnDefinition = "LONGTEXT")
    private String plainTextBody;

    @Column(name = "revised_by_email")
    private String revisedByEmail;

    @CreationTimestamp
    @Column(name = "revised_date", updatable = false)
    private LocalDateTime revisedDate;
}
