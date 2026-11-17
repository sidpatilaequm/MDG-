package com.example.multimedia.file_upload_api.controller.supplier;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.service.supplier.SupplierDocumentService;
import com.example.multimedia.file_upload_api.service.supplier.SupplierRegistrationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Authenticated reviewer endpoints - matches SecurityConfig's
 *  {@code /api/supplier-registration/**} rule (approver or super_admin only). */
@RestController
@RequestMapping("/api/supplier-registration")
public class SupplierRegistrationReviewController {

    private final SupplierRegistrationService registrationService;
    private final SupplierDocumentService documentService;

    public SupplierRegistrationReviewController(SupplierRegistrationService registrationService,
                                                  SupplierDocumentService documentService) {
        this.registrationService = registrationService;
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<ServiceResponse> listForReview() {
        return ResponseEntity.ok(registrationService.listForReview());
    }

    @GetMapping("/{registrationId}")
    public ResponseEntity<ServiceResponse> getRegistration(@PathVariable Long registrationId) {
        return ResponseEntity.ok(registrationService.getRegistration(registrationId));
    }

    @GetMapping("/documents/{documentId}/preview")
    public ResponseEntity<byte[]> previewDocument(@PathVariable Long documentId) {
        var file = documentService.getDocumentFile(documentId);
        return ResponseEntity.ok()
                .contentType(file.contentType() != null ? MediaType.parseMediaType(file.contentType()) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .body(file.data());
    }

    @GetMapping("/attachments/{attachmentId}/preview")
    public ResponseEntity<byte[]> previewAttachment(@PathVariable Long attachmentId) {
        var file = documentService.getAttachmentFile(attachmentId);
        return ResponseEntity.ok()
                .contentType(file.contentType() != null ? MediaType.parseMediaType(file.contentType()) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .body(file.data());
    }
}
