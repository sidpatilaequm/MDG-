package com.example.multimedia.file_upload_api.service;

import com.example.multimedia.file_upload_api.entity.CertificateOfIncorporation;
import com.example.multimedia.file_upload_api.entity.ChequeDetails;
import com.example.multimedia.file_upload_api.entity.CompanyDetails;
import com.example.multimedia.file_upload_api.entity.UserDetail;
import com.example.multimedia.file_upload_api.repository.CompanyDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared "list all companies under this tenant with a given authKey, flattened into a display
 * map" logic - previously duplicated almost byte-for-byte between VendorService.getAllVendors()
 * and CustomerService.getAllCustomers() (same CompanyDetails/UserDetail/ChequeDetails/
 * CertificateOfIncorporation shape, differing only by the authKey literal and the map's field
 * names, which were otherwise identical).
 */
@Service
public class CompanyDirectoryMappingService {

    @Autowired
    private CompanyDetailsRepository companyDetailsRepository;

    public List<Map<String, Object>> buildCompanyMapList(Long superAdminId, String authKey) {
        List<CompanyDetails> companies = companyDetailsRepository.findBySuperAdmin_SuperAdminIdAndAuthKey(superAdminId, authKey);

        List<Map<String, Object>> result = new ArrayList<>();
        for (CompanyDetails company : companies) {
            UserDetail user = company.getUser();

            // Only include active accounts
            if (user == null || !user.getIsActive()) {
                continue;
            }

            Map<String, Object> data = new HashMap<>();

            // Basic user info
            data.put("userId", user.getUserId());
            data.put("email", user.getEmail());
            data.put("firstName", user.getFirstName());
            data.put("lastName", user.getLastName());
            data.put("phoneNumber", user.getPhoneNumber());
            data.put("isActive", user.getIsActive());

            // Company info
            data.put("companyId", company.getCompanyId());
            data.put("companyName", company.getCompanyName());
            data.put("gstinNumber", company.getGstinNumber());
            data.put("legalTradeName", company.getLegalTradeName());
            data.put("registeredAddress", company.getRegisteredAddress());
            data.put("panNumber", company.getPanNumber());

            // Bank Details
            ChequeDetails chequeDetails = company.getChequeDetails();
            if (chequeDetails != null) {
                Map<String, String> bankDetails = new HashMap<>();
                bankDetails.put("accountNumber", chequeDetails.getAccountNumber());
                bankDetails.put("bankName", chequeDetails.getBank());
                bankDetails.put("branchName", chequeDetails.getBranch());
                bankDetails.put("ifscCode", chequeDetails.getIfsc());
                bankDetails.put("chequeCode", chequeDetails.getCode());
                data.put("bankDetails", bankDetails);
            }

            // COI Details
            CertificateOfIncorporation coi = company.getCertificateOfIncorporation();
            if (coi != null) {
                Map<String, String> coiDetails = new HashMap<>();
                coiDetails.put("cinNumber", coi.getCinNumber());
                coiDetails.put("createdDate", coi.getCreatedDate() != null ? coi.getCreatedDate().toString() : null);
                data.put("coiDetails", coiDetails);
            }

            // File names
            Map<String, String> fileNames = new HashMap<>();
            fileNames.put("gstFileName", company.getGstFileName());
            fileNames.put("panFileName", company.getPanFileName());
            fileNames.put("chequeFileName", company.getChequeFileName());
            fileNames.put("coiFileName", company.getCoiFileName());
            data.put("fileNames", fileNames);

            result.add(data);
        }
        return result;
    }
}
