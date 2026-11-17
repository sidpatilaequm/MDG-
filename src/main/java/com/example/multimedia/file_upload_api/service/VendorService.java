package com.example.multimedia.file_upload_api.service;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.entity.*;
import com.example.multimedia.file_upload_api.repository.*;
import com.example.multimedia.file_upload_api.utils.AppConstants;
import com.example.multimedia.file_upload_api.utils.ServiceControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class VendorService {

    private static final Logger logger = LoggerFactory.getLogger(VendorService.class);

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private CompanyDetailsRepository companyDetailsRepository;

    @Autowired
    private CompanyDirectoryMappingService companyDirectoryMappingService;

    @Autowired
    private ServiceControllerUtils serviceControllerUtils;

    @Autowired
    private CurrentUserService currentUserService;

    @Transactional
    public ServiceResponse getAllVendors() {
        ServiceResponse response = new ServiceResponse();

        try {
            Long currentAdminId = currentUserService.getCurrentSuperAdminId();
            List<Map<String, Object>> vendorList = companyDirectoryMappingService.buildCompanyMapList(currentAdminId, "vendor");
            logger.info("Found {} vendor companies", vendorList.size());

            response.addData("vendors", vendorList);
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(
                response,
                AppConstants.SUCCESSCODE,
                "Vendors retrieved successfully"
            );

        } catch (Exception e) {
            logger.error("Failed to retrieve vendors", e);
            return serviceControllerUtils.prepareMobileResponseErrorStatus(
                response,
                AppConstants.ERRORCODE,
                "Failed to retrieve vendors: " + e.getMessage()
            );
        }
    }

    @Transactional
    public ServiceResponse updateVendorAuthKey(Long userId, String authKey) {
        ServiceResponse response = new ServiceResponse();

        try {
            userDetailRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            CompanyDetails company = companyDetailsRepository.findByUserUserId(userId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Company details not found"));

            if (authKey != null && !authKey.isEmpty()) company.setAuthKey(authKey);

            company = companyDetailsRepository.save(company);

            // Prepare response
            Map<String, Object> vendorData = new HashMap<>();
            vendorData.put("companyId", company.getCompanyId());
            vendorData.put("authKey", company.getAuthKey());

            response.addData("vendor", vendorData);
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(
                response,
                AppConstants.SUCCESSCODE,
                "Vendor details updated successfully"
            );

        } catch (Exception e) {
            return serviceControllerUtils.prepareMobileResponseErrorStatus(
                response,
                AppConstants.ERRORCODE,
                "Failed to update vendor details: " + e.getMessage()
            );
        }
    }
}