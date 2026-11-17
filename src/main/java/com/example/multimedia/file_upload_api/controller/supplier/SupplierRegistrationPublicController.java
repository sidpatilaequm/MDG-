package com.example.multimedia.file_upload_api.controller.supplier;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.service.supplier.SupplierDocumentService;
import com.example.multimedia.file_upload_api.service.supplier.SupplierRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Public, no-login "Become a Supplier" flow - draft save/resume, per-document-type upload
 * (with OCR + Microvista verification), free-form attachments, and final submit into the
 * approval workflow. Matches SecurityConfig's existing {@code /api/public/**} permitAll rule.
 */
@RestController
@RequestMapping("/api/public/supplier-registration")
public class SupplierRegistrationPublicController {

    private final SupplierRegistrationService registrationService;
    private final SupplierDocumentService documentService;

    public SupplierRegistrationPublicController(SupplierRegistrationService registrationService,
                                                  SupplierDocumentService documentService) {
        this.registrationService = registrationService;
        this.documentService = documentService;
    }

    @PostMapping("/draft")
    public ResponseEntity<ServiceResponse> saveDraft(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(registrationService.saveDraft(body));
    }

    @GetMapping("/draft/{resumeCode}")
    public ResponseEntity<ServiceResponse> getDraft(@PathVariable String resumeCode) {
        return ResponseEntity.ok(registrationService.getDraftByCode(resumeCode));
    }

    @PostMapping("/{registrationId}/documents/{docType}")
    public ResponseEntity<ServiceResponse> uploadDocument(@PathVariable Long registrationId,
                                                           @PathVariable String docType,
                                                           @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentService.uploadDocument(registrationId, docType, file));
    }

    @PostMapping("/{registrationId}/attachments")
    public ResponseEntity<ServiceResponse> uploadAttachment(@PathVariable Long registrationId,
                                                             @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentService.uploadAttachment(registrationId, file));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<ServiceResponse> removeAttachment(@PathVariable Long attachmentId) {
        return ResponseEntity.ok(documentService.removeAttachment(attachmentId));
    }

    @PostMapping("/{registrationId}/submit")
    public ResponseEntity<ServiceResponse> submit(@PathVariable Long registrationId) {
        return ResponseEntity.ok(registrationService.submit(registrationId));
    }
}
