package com.example.multimedia.file_upload_api.service.impl;

import com.example.multimedia.file_upload_api.dto.PermissionItemDto;
import com.example.multimedia.file_upload_api.dto.VendorPermissionRequestDto;
import com.example.multimedia.file_upload_api.dto.VendorPermissionResponseDto;
import com.example.multimedia.file_upload_api.entity.CompanyDetails;
import com.example.multimedia.file_upload_api.entity.PermissionMaster;
import com.example.multimedia.file_upload_api.entity.UserDetail;
import com.example.multimedia.file_upload_api.entity.VendorPermission;
import com.example.multimedia.file_upload_api.repository.CompanyDetailsRepository;
import com.example.multimedia.file_upload_api.repository.PermissionMasterRepository;
import com.example.multimedia.file_upload_api.repository.VendorPermissionRepository;
import com.example.multimedia.file_upload_api.service.CurrentUserService;
import com.example.multimedia.file_upload_api.service.VendorPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Ported from backend_java's VendorPermissionServiceImpl. Two adaptations for MDG-:
 * - MDG-'s UserDetail has no "company" field (unlike backend_java's) - "my own company" is
 *   resolved via CompanyDetailsRepository.findByUserUserId(...), the same reverse lookup
 *   VendorService already uses elsewhere in this codebase.
 * - No AuditLogService exists in MDG- - replaced with a plain log line.
 */
@Service
public class VendorPermissionServiceImpl implements VendorPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(VendorPermissionServiceImpl.class);

    @Autowired
    private VendorPermissionRepository vendorPermissionRepository;

    @Autowired
    private PermissionMasterRepository permissionMasterRepository;

    @Autowired
    private CompanyDetailsRepository companyDetailsRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    @Transactional
    public void saveVendorPermissions(VendorPermissionRequestDto requestDto) {
        Long currentSuperAdminId = currentUserService.getCurrentSuperAdminId();
        CompanyDetails company = companyDetailsRepository.findById(requestDto.getVendorId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Tenant Isolation Check
        if (company.getSuperAdmin() == null || !company.getSuperAdmin().getSuperAdminId().equals(currentSuperAdminId)) {
            throw new RuntimeException("Access Denied: You do not own this vendor company");
        }

        for (PermissionItemDto item : requestDto.getPermissions()) {
            PermissionMaster permissionMaster = permissionMasterRepository.findByCode(item.getPermissionCode())
                    .orElseThrow(() -> new RuntimeException("Permission code not found: " + item.getPermissionCode()));

            VendorPermission vp = vendorPermissionRepository
                    .findByCompanyCompanyIdAndPermissionCode(company.getCompanyId(), item.getPermissionCode())
                    .orElse(new VendorPermission());

            vp.setCompany(company);
            vp.setPermission(permissionMaster);
            vp.setCanView(item.getView() != null && item.getView());
            vp.setCanCreate(item.getCreate() != null && item.getCreate());
            vp.setCanEdit(item.getEdit() != null && item.getEdit());
            vp.setCanDelete(item.getDelete() != null && item.getDelete());

            vendorPermissionRepository.save(vp);
        }

        logger.info("Vendor permissions updated for company {} ({}) - {} entries",
                company.getCompanyId(), company.getCompanyName(), requestDto.getPermissions().size());
    }

    @Override
    public VendorPermissionResponseDto getVendorPermissions(Long companyId) {
        Long currentSuperAdminId = currentUserService.getCurrentSuperAdminId();
        CompanyDetails company = companyDetailsRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Tenant Isolation Check
        if (company.getSuperAdmin() == null || !company.getSuperAdmin().getSuperAdminId().equals(currentSuperAdminId)) {
            throw new RuntimeException("Access Denied: You do not own this vendor company");
        }

        return buildPermissionTree(companyId, company.getCompanyName());
    }

    @Override
    public VendorPermissionResponseDto getMyPermissions() {
        UserDetail currentUser = currentUserService.getCurrentUser();

        CompanyDetails company = companyDetailsRepository.findByUserUserId(currentUser.getUserId())
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No company found for current user"));

        return buildPermissionTree(company.getCompanyId(), company.getCompanyName());
    }

    @Override
    public VendorPermissionResponseDto getPermissionsForLogin(Long companyId) {
        // Skips the SuperAdmin ownership check for the login flow.
        String companyName = "Vendor Portal";
        Optional<CompanyDetails> companyOpt = companyDetailsRepository.findById(companyId);
        if (companyOpt.isPresent()) {
            companyName = companyOpt.get().getCompanyName();
        }
        return buildPermissionTree(companyId, companyName);
    }

    private VendorPermissionResponseDto buildPermissionTree(Long companyId, String companyName) {
        List<PermissionMaster> masters = permissionMasterRepository.findAll();

        List<VendorPermission> assigned = vendorPermissionRepository.findByCompanyCompanyId(companyId);
        Map<String, VendorPermission> assignedMap = assigned.stream()
                .collect(Collectors.toMap(vp -> vp.getPermission().getCode(), vp -> vp));

        Map<String, PermissionItemDto> dtoMap = new HashMap<>();
        for (PermissionMaster m : masters) {
            VendorPermission vp = assignedMap.get(m.getCode());
            PermissionItemDto dto = new PermissionItemDto();
            dto.setPermissionId(m.getId());
            dto.setPermissionCode(m.getCode());
            dto.setPermissionName(m.getName());
            dto.setPermissionType(m.getType().name());
            dto.setView(vp != null && vp.getCanView());
            dto.setCreate(vp != null && vp.getCanCreate());
            dto.setEdit(vp != null && vp.getCanEdit());
            dto.setDelete(vp != null && vp.getCanDelete());
            dto.setChildren(new ArrayList<>());
            dtoMap.put(m.getCode(), dto);
        }

        List<PermissionItemDto> roots = new ArrayList<>();
        for (PermissionMaster m : masters) {
            PermissionItemDto dto = dtoMap.get(m.getCode());
            if (m.getParent() == null) {
                roots.add(dto);
            } else {
                PermissionItemDto parentDto = dtoMap.get(m.getParent().getCode());
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                }
            }
        }

        return VendorPermissionResponseDto.builder()
                .vendorId(companyId)
                .vendorName(companyName)
                .permissions(roots)
                .build();
    }

    @Override
    public boolean hasPermission(Long companyId, String permissionCode, String action) {
        Optional<VendorPermission> vpOpt = vendorPermissionRepository
                .findByCompanyCompanyIdAndPermissionCode(companyId, permissionCode);

        if (vpOpt.isEmpty()) return false;

        VendorPermission vp = vpOpt.get();
        return switch (action.toUpperCase()) {
            case "VIEW" -> vp.getCanView();
            case "CREATE" -> vp.getCanCreate();
            case "EDIT" -> vp.getCanEdit();
            case "DELETE" -> vp.getCanDelete();
            default -> false;
        };
    }
}
