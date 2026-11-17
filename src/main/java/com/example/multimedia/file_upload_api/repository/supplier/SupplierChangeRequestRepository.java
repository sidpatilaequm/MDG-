package com.example.multimedia.file_upload_api.repository.supplier;

import com.example.multimedia.file_upload_api.entity.supplier.SupplierChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierChangeRequestRepository extends JpaRepository<SupplierChangeRequest, Long> {

    List<SupplierChangeRequest> findByRegistration_Id(Long registrationId);

    Optional<SupplierChangeRequest> findByWorkflowRequest_Id(Long workflowRequestId);
}
