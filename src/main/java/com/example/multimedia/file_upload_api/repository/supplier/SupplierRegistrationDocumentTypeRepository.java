package com.example.multimedia.file_upload_api.repository.supplier;

import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistrationDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRegistrationDocumentTypeRepository extends JpaRepository<SupplierRegistrationDocumentType, SupplierRegistrationDocumentType.Pk> {

    List<SupplierRegistrationDocumentType> findByRegistrationId(Long registrationId);
}
