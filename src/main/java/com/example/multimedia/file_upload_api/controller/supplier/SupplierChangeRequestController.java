package com.example.multimedia.file_upload_api.controller.supplier;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.service.supplier.SupplierChangeRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** An approved vendor requesting a change to their own profile - matches SecurityConfig's
 *  {@code /api/vendor-change-requests/**} rule (vendor or super_admin only); ownership of the
 *  target registration is additionally enforced in SupplierChangeRequestService. */
@RestController
@RequestMapping("/api/vendor-change-requests")
public class SupplierChangeRequestController {

    private final SupplierChangeRequestService changeRequestService;

    public SupplierChangeRequestController(SupplierChangeRequestService changeRequestService) {
        this.changeRequestService = changeRequestService;
    }

    @PostMapping("/{registrationId}")
    public ResponseEntity<ServiceResponse> submit(@PathVariable Long registrationId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(changeRequestService.submit(registrationId, body.get("fieldName"), body.get("newValue")));
    }
}
