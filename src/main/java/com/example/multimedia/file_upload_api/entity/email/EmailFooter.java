package com.example.multimedia.file_upload_api.entity.email;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Single global footer (e.g. company address/unsubscribe note) appended to every rendered
 *  email - a singleton row (id=1) rather than one per template. */
@Entity
@Table(name = "email_footer")
@Getter
@Setter
public class EmailFooter {

    @Id
    private Long id = 1L;

    @Column(name = "html_footer", columnDefinition = "LONGTEXT")
    private String htmlFooter;

    @Column(name = "plain_text_footer", columnDefinition = "TEXT")
    private String plainTextFooter;

    @UpdateTimestamp
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
