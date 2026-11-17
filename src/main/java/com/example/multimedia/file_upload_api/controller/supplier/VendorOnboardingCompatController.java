package com.example.multimedia.file_upload_api.controller.supplier;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.service.supplier.VendorOnboardingCompatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Exposes MDG-'s existing supplier-registration + approval-workflow engine under the exact
 * paths/payloads Django's vendor-onboarding screens already call (pages/views.py). Public
 * endpoints match SecurityConfig's existing {@code /api/public/**} permitAll rule; the
 * authenticated ones match the new {@code /api/vendor-onboarding/**} rule (approver or
 * super_admin).
 */
@RestController
public class VendorOnboardingCompatController {

    private final VendorOnboardingCompatService onboardingCompatService;

    public VendorOnboardingCompatController(VendorOnboardingCompatService onboardingCompatService) {
        this.onboardingCompatService = onboardingCompatService;
    }

    @PostMapping("/api/public/registration/onboarding-request")
    public ResponseEntity<ServiceResponse> onboardingRequest(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(onboardingCompatService.onboardingRequest(body));
    }

    @GetMapping("/api/vendor-onboarding/requests")
    public ResponseEntity<ServiceResponse> listRequests() {
        return ResponseEntity.ok(onboardingCompatService.listRequests());
    }

    @GetMapping("/api/public/registration/onboarding-details")
    public ResponseEntity<ServiceResponse> onboardingDetails(@RequestParam String token) {
        return ResponseEntity.ok(onboardingCompatService.onboardingDetails(token));
    }

    @PostMapping("/api/vendor-onboarding/generate-link/{user_id}")
    public ResponseEntity<ServiceResponse> generateLink(@PathVariable("user_id") Long userId) {
        return ResponseEntity.ok(onboardingCompatService.generateLink(userId));
    }
}
