package com.example.multimedia.file_upload_api.service;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.utils.AppConstants;
import com.example.multimedia.file_upload_api.utils.ServiceControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CompanyDirectoryMappingService companyDirectoryMappingService;

    @Autowired
    private ServiceControllerUtils serviceControllerUtils;

    @Autowired
    private CurrentUserService currentUserService;

    @Transactional
    public ServiceResponse getAllCustomers() {
        ServiceResponse response = new ServiceResponse();

        try {
            Long currentAdminId = currentUserService.getCurrentSuperAdminId();
            List<Map<String, Object>> customerList = companyDirectoryMappingService.buildCompanyMapList(currentAdminId, "customer");
            logger.info("Found {} customer companies", customerList.size());

            response.addData("customers", customerList);
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(
                response,
                AppConstants.SUCCESSCODE,
                "Customers retrieved successfully"
            );

        } catch (Exception e) {
            logger.error("Failed to retrieve customers", e);
            return serviceControllerUtils.prepareMobileResponseErrorStatus(
                response,
                AppConstants.ERRORCODE,
                "Failed to retrieve customers: " + e.getMessage()
            );
        }
    }
}