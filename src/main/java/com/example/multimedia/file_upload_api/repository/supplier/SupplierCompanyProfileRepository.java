package com.example.multimedia.file_upload_api.repository.supplier;

import com.example.multimedia.file_upload_api.entity.supplier.SupplierCompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierCompanyProfileRepository extends JpaRepository<SupplierCompanyProfile, Long> {

    Optional<SupplierCompanyProfile> findByCompany_CompanyId(Long companyId);

    Optional<SupplierCompanyProfile> findByRegistration_Id(Long registrationId);
}
