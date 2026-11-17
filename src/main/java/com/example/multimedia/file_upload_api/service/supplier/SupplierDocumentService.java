package com.example.multimedia.file_upload_api.service.supplier;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.dto.supplier.VerifyResult;
import com.example.multimedia.file_upload_api.entity.supplier.*;
import com.example.multimedia.file_upload_api.repository.supplier.*;
import com.example.multimedia.file_upload_api.util.SupplierDocumentConfig;
import com.example.multimedia.file_upload_api.util.SupplierDocumentConfig.DocDef;
import com.example.multimedia.file_upload_api.utils.AppConstants;
import com.example.multimedia.file_upload_api.utils.ServiceControllerUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Per-document-type upload for a supplier registration: runs OCR (OcrExtractionService),
 * mirrors whatever it extracted back onto the registration's own KYC columns, dispatches the
 * matching Microvista verification call if the DocDef defines one, and stores the file bytes
 * in the document's blob side table.
 */
@Service
public class SupplierDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierDocumentService.class);

    private static final Set<SupplierRegistrationStatus> EDITABLE_STATUSES =
            Set.of(SupplierRegistrationStatus.DRAFT, SupplierRegistrationStatus.REJECTED);

    private final SupplierRegistrationRepository registrationRepository;
    private final SupplierRegistrationDocumentRepository documentRepository;
    private final SupplierRegistrationDocumentBlobRepository documentBlobRepository;
    private final SupplierRegistrationAttachmentRepository attachmentRepository;
    private final SupplierRegistrationAttachmentBlobRepository attachmentBlobRepository;
    private final OcrExtractionService ocrExtractionService;
    private final MicrovistaService microvistaService;
    private final ServiceControllerUtils serviceControllerUtils;

    public SupplierDocumentService(SupplierRegistrationRepository registrationRepository,
                                    SupplierRegistrationDocumentRepository documentRepository,
                                    SupplierRegistrationDocumentBlobRepository documentBlobRepository,
                                    SupplierRegistrationAttachmentRepository attachmentRepository,
                                    SupplierRegistrationAttachmentBlobRepository attachmentBlobRepository,
                                    OcrExtractionService ocrExtractionService,
                                    MicrovistaService microvistaService,
                                    ServiceControllerUtils serviceControllerUtils) {
        this.registrationRepository = registrationRepository;
        this.documentRepository = documentRepository;
        this.documentBlobRepository = documentBlobRepository;
        this.attachmentRepository = attachmentRepository;
        this.attachmentBlobRepository = attachmentBlobRepository;
        this.ocrExtractionService = ocrExtractionService;
        this.microvistaService = microvistaService;
        this.serviceControllerUtils = serviceControllerUtils;
    }

    @Transactional
    public ServiceResponse uploadDocument(Long registrationId, String docType, MultipartFile file) {
        ServiceResponse response = new ServiceResponse();
        try {
            SupplierRegistration reg = registrationRepository.findById(registrationId)
                    .orElseThrow(() -> new RuntimeException("Registration not found"));
            if (!EDITABLE_STATUSES.contains(reg.getStatus())) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        "This application has already been submitted and its documents can no longer be changed.");
            }

            DocDef docDef = SupplierDocumentConfig.byId(reg.getVendorType(), docType);
            JSONObject ocrFields = ocrExtractionService.extract(file, docDef);
            mirrorFieldsOntoRegistration(reg, ocrFields);
            registrationRepository.save(reg);

            SupplierRegistrationDocument doc = documentRepository
                    .findByRegistration_IdAndDocType(registrationId, docType)
                    .orElseGet(() -> {
                        SupplierRegistrationDocument d = new SupplierRegistrationDocument();
                        d.setRegistration(reg);
                        d.setDocType(docType);
                        return d;
                    });
            doc.setFileName(file.getOriginalFilename());
            doc.setContentType(file.getContentType());
            doc.setOcrExtractedFieldsJson(ocrFields.toString());
            doc.setVerifyStatus("read");
            doc = documentRepository.save(doc);

            SupplierRegistrationDocumentBlob blob = documentBlobRepository.findById(doc.getId())
                    .orElseGet(SupplierRegistrationDocumentBlob::new);
            blob.setDocument(doc);
            blob.setFileData(file.getBytes());
            documentBlobRepository.save(blob);

            if (docDef.verifyKind() != null) {
                VerifyResult result = dispatchVerify(docDef.verifyKind(), reg);
                if (result != null) {
                    doc.setVerifyStatus(result.isVerified() ? "verified" : "error");
                    JSONObject details = new JSONObject();
                    details.put("verified", result.isVerified());
                    details.put("message", result.getMessage());
                    doc.setVerifyDetailsJson(details.toString());
                    documentRepository.save(doc);
                }
            }

            response.addData("documentId", doc.getId());
            response.addData("ocrExtractedFields", ocrFields.toMap());
            response.addData("verifyStatus", doc.getVerifyStatus());
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Document uploaded");
        } catch (Exception e) {
            logger.error("Document upload failed for registrationId={}, docType={}", registrationId, docType, e);
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, "Upload failed: " + e.getMessage());
        }
    }

    private void mirrorFieldsOntoRegistration(SupplierRegistration reg, JSONObject fields) {
        fields.keySet().forEach(key -> {
            String value = fields.optString(key, null);
            if (value == null || value.isBlank()) return;
            switch (key) {
                case "pan" -> reg.setPanNumber(value);
                case "gstin" -> reg.setGstNumber(value);
                case "cin" -> reg.setCinNumber(value);
                case "udyam" -> reg.setMsmeNumber(value);
                case "acctNo" -> reg.setAccountNumber(value);
                case "ifsc" -> reg.setIfscCode(value);
                case "benName" -> reg.setBeneficiaryName(value);
                case "foreignTaxId" -> reg.setForeignTaxId(value);
                case "foreignRegNo" -> reg.setForeignRegNo(value);
                case "foreignAcctNo" -> reg.setForeignAcctNo(value);
                case "swift" -> reg.setSwiftCode(value);
                case "isoNo" -> reg.setIsoCertificateNo(value);
                case "isoBody" -> reg.setIsoCertifyingBody(value);
                case "isoExpiry" -> reg.setIsoExpiry(value);
                case "iso14001No" -> reg.setIso14001CertificateNo(value);
                case "iso14001Body" -> reg.setIso14001CertifyingBody(value);
                case "iso14001Expiry" -> reg.setIso14001Expiry(value);
                case "iso45001No" -> reg.setIso45001CertificateNo(value);
                case "iso45001Body" -> reg.setIso45001CertifyingBody(value);
                case "iso45001Expiry" -> reg.setIso45001Expiry(value);
                case "iso27001No" -> reg.setIso27001CertificateNo(value);
                case "iso27001Body" -> reg.setIso27001CertifyingBody(value);
                case "iso27001Expiry" -> reg.setIso27001Expiry(value);
                case "asNo" -> reg.setAs9100dCertificateNo(value);
                case "asBody" -> reg.setAs9100dCertifyingBody(value);
                case "asExpiry" -> reg.setAs9100dExpiry(value);
                case "nadcapNo" -> reg.setNadcapCertificateNo(value);
                case "nadcapExpiry" -> reg.setNadcapExpiry(value);
                default -> { /* no matching registration column - stays OCR-only */ }
            }
        });
    }

    private VerifyResult dispatchVerify(String verifyKind, SupplierRegistration reg) {
        try {
            return switch (verifyKind) {
                case "pan" -> isBlank(reg.getPanNumber()) ? null : microvistaService.verifyPan(reg.getPanNumber());
                case "gstin" -> isBlank(reg.getGstNumber()) ? null : microvistaService.verifyGstin(reg.getGstNumber());
                case "cin" -> isBlank(reg.getCinNumber()) ? null : microvistaService.verifyCin(reg.getCinNumber());
                case "udyam" -> isBlank(reg.getMsmeNumber()) ? null : microvistaService.verifyUdyam(reg.getMsmeNumber());
                case "bank" -> (isBlank(reg.getAccountNumber()) || isBlank(reg.getIfscCode())) ? null
                        : microvistaService.verifyBank(reg.getAccountNumber(), reg.getIfscCode(), reg.getBeneficiaryName());
                default -> null;
            };
        } catch (Exception e) {
            logger.warn("Verification dispatch failed for kind={} on registration {}", verifyKind, reg.getId(), e);
            return VerifyResult.failed("Could not complete verification: " + e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Transactional
    public ServiceResponse uploadAttachment(Long registrationId, MultipartFile file) {
        ServiceResponse response = new ServiceResponse();
        try {
            SupplierRegistration reg = registrationRepository.findById(registrationId)
                    .orElseThrow(() -> new RuntimeException("Registration not found"));
            if (!EDITABLE_STATUSES.contains(reg.getStatus())) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        "This application has already been submitted and its attachments can no longer be changed.");
            }

            SupplierRegistrationAttachment attachment = new SupplierRegistrationAttachment();
            attachment.setRegistration(reg);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setContentType(file.getContentType());
            attachment = attachmentRepository.save(attachment);

            SupplierRegistrationAttachmentBlob blob = new SupplierRegistrationAttachmentBlob();
            blob.setAttachment(attachment);
            blob.setFileData(file.getBytes());
            attachmentBlobRepository.save(blob);

            response.addData("attachmentId", attachment.getId());
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Attachment uploaded");
        } catch (Exception e) {
            logger.error("Attachment upload failed for registrationId={}", registrationId, e);
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, "Upload failed: " + e.getMessage());
        }
    }

    public record DownloadedFile(String fileName, String contentType, byte[] data) {}

    public DownloadedFile getDocumentFile(Long documentId) {
        SupplierRegistrationDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        SupplierRegistrationDocumentBlob blob = documentBlobRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document file not found"));
        return new DownloadedFile(doc.getFileName(), doc.getContentType(), blob.getFileData());
    }

    public DownloadedFile getAttachmentFile(Long attachmentId) {
        SupplierRegistrationAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        SupplierRegistrationAttachmentBlob blob = attachmentBlobRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment file not found"));
        return new DownloadedFile(attachment.getFileName(), attachment.getContentType(), blob.getFileData());
    }

    @Transactional
    public ServiceResponse removeAttachment(Long attachmentId) {
        ServiceResponse response = new ServiceResponse();
        try {
            SupplierRegistrationAttachment attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Attachment not found"));
            if (!EDITABLE_STATUSES.contains(attachment.getRegistration().getStatus())) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        "This application has already been submitted and its attachments can no longer be changed.");
            }
            attachmentBlobRepository.deleteById(attachmentId);
            attachmentRepository.delete(attachment);
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Attachment removed");
        } catch (Exception e) {
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, e.getMessage());
        }
    }
}
