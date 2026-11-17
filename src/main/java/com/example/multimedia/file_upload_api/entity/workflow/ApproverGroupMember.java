package com.example.multimedia.file_upload_api.entity.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Plain userId reference (not a hard FK) so this stays usable whether the member is a
 *  UserDetail or (in principle) a SuperAdmin - both share the id space of "who can log in". */
@Entity
@Table(name = "approver_group_member")
@Getter
@Setter
public class ApproverGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "approver_group_id", nullable = false)
    private ApproverGroup approverGroup;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
}
