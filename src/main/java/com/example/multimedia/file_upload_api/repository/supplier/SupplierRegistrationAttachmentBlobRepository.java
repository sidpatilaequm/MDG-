package com.example.multimedia.file_upload_api.repository.supplier;

import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistrationAttachmentBlob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRegistrationAttachmentBlobRepository extends JpaRepository<SupplierRegistrationAttachmentBlob, Long> {
}
