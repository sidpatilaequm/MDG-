package com.example.multimedia.file_upload_api.service.supplier;

import com.example.multimedia.file_upload_api.util.SupplierDocumentConfig.DocDef;
import com.example.multimedia.file_upload_api.util.SupplierDocumentConfig.FieldDef;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-process OCR for supplier-registration documents, over the already-present tess4j
 * dependency (no external OCR microservice, unlike backend_java's separate Python/GPT-vision
 * dependency). Extracts whichever of a {@link DocDef}'s fields a well-known regex covers (PAN,
 * GSTIN, CIN, Udyam, IFSC, account number) - everything else comes back blank and stays
 * editable by the applicant, since OCR here is a convenience, never a hard requirement.
 *
 * A fresh {@link Tesseract} instance is created per extraction rather than shared as a
 * singleton bean - tess4j's native instances are not safe for concurrent use across threads.
 */
@Service
public class OcrExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(OcrExtractionService.class);

    private static final Pattern PAN_PATTERN = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");
    private static final Pattern GSTIN_PATTERN = Pattern.compile("[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}");
    private static final Pattern CIN_PATTERN = Pattern.compile("[LUlu][0-9]{5}[A-Za-z]{2}[0-9]{4}[A-Za-z]{3}[0-9]{6}");
    private static final Pattern UDYAM_PATTERN = Pattern.compile("UDYAM-[A-Z]{2}-[0-9]{2}-[0-9]{7}", Pattern.CASE_INSENSITIVE);
    private static final Pattern IFSC_PATTERN = Pattern.compile("[A-Z]{4}0[A-Z0-9]{6}");
    private static final Pattern ACCOUNT_NO_PATTERN = Pattern.compile("\\b[0-9]{9,18}\\b");

    private final String tessdataPath;

    public OcrExtractionService(@Value("${ocr.tessdata.path}") String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    /** Runs OCR against the file, then extracts whichever of docDef's fields have a known
     *  pattern. Returns a JSON object of {fieldKey: extractedValue}, never null - unmatched
     *  fields are simply absent from the object. */
    public JSONObject extract(MultipartFile file, DocDef docDef) {
        JSONObject result = new JSONObject();
        try {
            String text = runOcr(file);
            for (FieldDef field : docDef.fields()) {
                String value = extractField(text, field.key());
                if (value != null) result.put(field.key(), value);
            }
        } catch (Throwable t) {
            // Throwable, not Exception: a missing/misconfigured native tesseract library
            // surfaces as UnsatisfiedLinkError (an Error, not an Exception) - OCR is a
            // convenience here, never a hard requirement, so even that must degrade to an
            // empty result instead of failing the whole upload request.
            logger.warn("OCR extraction failed for docType={}", docDef.id(), t);
        }
        return result;
    }

    private String runOcr(MultipartFile file) throws Exception {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("eng");
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IllegalArgumentException("Could not read uploaded file as an image for OCR");
        }
        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            throw new RuntimeException("Tesseract OCR failed", e);
        }
    }

    private String extractField(String text, String fieldKey) {
        Pattern pattern = switch (fieldKey) {
            case "pan" -> PAN_PATTERN;
            case "gstin" -> GSTIN_PATTERN;
            case "cin" -> CIN_PATTERN;
            case "udyam" -> UDYAM_PATTERN;
            case "ifsc" -> IFSC_PATTERN;
            case "acctNo" -> ACCOUNT_NO_PATTERN;
            default -> null;
        };
        if (pattern == null) return null;
        Matcher matcher = pattern.matcher(text.toUpperCase());
        return matcher.find() ? matcher.group() : null;
    }
}
