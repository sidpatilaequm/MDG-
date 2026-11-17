package com.example.multimedia.file_upload_api.repository;

import com.example.multimedia.file_upload_api.entity.ChequeDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChequeDetailsRepository extends JpaRepository<ChequeDetails, Long> {
    ChequeDetails findByAccountNumber(String accountNumber);

    Optional<ChequeDetails> findByCompany_CompanyId(Long companyId);
}