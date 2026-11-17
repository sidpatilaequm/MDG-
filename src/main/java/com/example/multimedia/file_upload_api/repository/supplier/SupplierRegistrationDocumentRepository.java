package com.example.multimedia.file_upload_api.repository.supplier;

import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistrationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRegistrationDocumentRepository extends JpaRepository<SupplierRegistrationDocument, Long> {

    Optional<SupplierRegistrationDocument> findByRegistration_IdAndDocType(Long registrationId, String docType);

    List<SupplierRegistrationDocument> findByRegistration_Id(Long registrationId);
}
