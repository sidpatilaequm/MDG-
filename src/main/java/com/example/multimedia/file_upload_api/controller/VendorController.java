package com.example.multimedia.file_upload_api.controller;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vendors")
@CrossOrigin(origins = "*")
public class VendorController {

    @Autowired
    private VendorService vendorService;

    @GetMapping("/all")
    public ResponseEntity<ServiceResponse> getAllVendors() {
        ServiceResponse response = vendorService.getAllVendors();
        return ResponseEntity.ok(response);
    }

    /** Despite historically accepting a full CompanyDetailsDTO, this endpoint only ever
     *  applied authKey - narrowed to match what it actually does. */
    @PutMapping("/{userId}")
    public ResponseEntity<ServiceResponse> updateVendorDetails(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        ServiceResponse response = vendorService.updateVendorAuthKey(userId, body.get("authKey"));
        return ResponseEntity.ok(response);
    }
}
