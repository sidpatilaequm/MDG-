package com.example.multimedia.file_upload_api.repository.supplier;

import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistration;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRegistrationRepository extends JpaRepository<SupplierRegistration, Long> {

    Optional<SupplierRegistration> findByResumeCode(String resumeCode);

    Optional<SupplierRegistration> findByEmail(String email);

    Optional<SupplierRegistration> findByWorkflowRequest_Id(Long workflowRequestId);

    List<SupplierRegistration> findByStatus(SupplierRegistrationStatus status);

    List<SupplierRegistration> findBySuperAdmin_SuperAdminIdAndStatus(Long superAdminId, SupplierRegistrationStatus status);

    /** Atomic claim so a redelivered/duplicate approval-resolution event can only provision the
     *  vendor account once, even under concurrent processing. Returns 1 if this call won the
     *  claim, 0 if another call already provisioned (or is provisioning) this registration. */
    @Modifying
    @Query("UPDATE SupplierRegistration r SET r.userId = -1 WHERE r.id = :id AND r.userId IS NULL")
    int claimProvisioning(@Param("id") Long id);
}
