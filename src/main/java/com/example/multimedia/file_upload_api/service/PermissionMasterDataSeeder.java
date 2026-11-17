package com.example.multimedia.file_upload_api.service;

import com.example.multimedia.file_upload_api.entity.PermissionMaster;
import com.example.multimedia.file_upload_api.enums.PermissionType;
import com.example.multimedia.file_upload_api.repository.PermissionMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the flat set of MODULE-level PermissionMaster rows Django's vendor-permissions
 * screen renders (confirmed by reading templates/partials/vendor_permissions_table.html on
 * the live server - these 7 module names are the only ones it hardcodes). Insert-if-missing
 * by code, safe on every boot.
 */
@Component
public class PermissionMasterDataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(PermissionMasterDataSeeder.class);

    private static final String[][] MODULES = {
            {"PURCHASE_REQUISITION", "Purchase Requisition"},
            {"PURCHASE_ORDER", "Purchase Order"},
            {"SUBCONTRACTING_PURCHASE_ORDER", "Subcontracting Purchase Order"},
            {"SCHEDULING_AGREEMENT", "Scheduling Agreement"},
            {"SERVICE_PURCHASE_ORDER", "Service Purchase Order"},
            {"INVENTORY", "Inventory"},
            {"VENDOR_PAYMENTS", "Vendor Payments"},
    };

    private final PermissionMasterRepository permissionMasterRepository;

    public PermissionMasterDataSeeder(PermissionMasterRepository permissionMasterRepository) {
        this.permissionMasterRepository = permissionMasterRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        int seeded = 0;
        for (String[] module : MODULES) {
            if (permissionMasterRepository.findByCode(module[0]).isPresent()) continue;
            PermissionMaster m = new PermissionMaster();
            m.setCode(module[0]);
            m.setName(module[1]);
            m.setType(PermissionType.MODULE);
            permissionMasterRepository.save(m);
            seeded++;
        }
        if (seeded > 0) {
            logger.info("Seeded {} vendor-permission module(s)", seeded);
        }
    }
}
