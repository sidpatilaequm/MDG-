package com.example.multimedia.file_upload_api.util;

import java.util.List;
import java.util.Map;

/**
 * Single source of truth for which fields OCR should extract per document type, and which
 * Microvista call (if any) verifies them. Ported from backend_java's SupplierDocumentConfig -
 * same field keys, same two lists (DOMESTIC vs FOREIGN vendorType).
 */
public final class SupplierDocumentConfig {

    public record FieldDef(String key, String label, boolean isDate) {
        public FieldDef(String key, String label) {
            this(key, label, false);
        }
    }

    public record DocDef(String id, String name, boolean required, List<FieldDef> fields, String verifyKind, List<String> doubleCheck) {}

    public static final List<DocDef> DOCS_DOMESTIC = List.of(
            new DocDef("coi", "Certificate of incorporation", false,
                    List.of(new FieldDef("cin", "CIN or LLPIN")), "cin", List.of("cin")),
            new DocDef("gst", "GST registration certificate", true,
                    List.of(new FieldDef("gstin", "GSTIN")), "gstin", List.of("gstin")),
            new DocDef("pan", "PAN card", true,
                    List.of(new FieldDef("pan", "PAN")), "pan", List.of("pan")),
            new DocDef("chq", "Cancelled cheque", true,
                    List.of(new FieldDef("benName", "Account name as printed"),
                            new FieldDef("acctNo", "Account number"),
                            new FieldDef("ifsc", "IFSC")),
                    "bank", List.of("acctNo", "ifsc")),
            new DocDef("udyam", "MSME / Udyam certificate", false,
                    List.of(new FieldDef("udyam", "Udyam registration number")), "udyam", List.of("udyam")),
            new DocDef("iso", "ISO 9001 certificate", false,
                    List.of(new FieldDef("isoNo", "Certificate number"),
                            new FieldDef("isoBody", "Certifying body"),
                            new FieldDef("isoExpiry", "Valid to", true)),
                    null, List.of()),
            new DocDef("iso14001", "ISO 14001 certificate", false,
                    List.of(new FieldDef("iso14001No", "Certificate number"),
                            new FieldDef("iso14001Body", "Certifying body"),
                            new FieldDef("iso14001Expiry", "Valid to", true)),
                    null, List.of()),
            new DocDef("iso45001", "ISO 45001 certificate", false,
                    List.of(new FieldDef("iso45001No", "Certificate number"),
                            new FieldDef("iso45001Body", "Certifying body"),
                            new FieldDef("iso45001Expiry", "Valid to", true)),
                    null, List.of()),
            new DocDef("iso27001", "ISO 27001 certificate", false,
                    List.of(new FieldDef("iso27001No", "Certificate number"),
                            new FieldDef("iso27001Body", "Certifying body"),
                            new FieldDef("iso27001Expiry", "Valid to", true)),
                    null, List.of()),
            new DocDef("as", "AS9100D certificate", false,
                    List.of(new FieldDef("asNo", "Certificate number"),
                            new FieldDef("asBody", "Certifying body"),
                            new FieldDef("asExpiry", "Valid to", true)),
                    null, List.of()),
            new DocDef("nadcap", "NADCAP certificate", false,
                    List.of(new FieldDef("nadcapNo", "Certificate number"),
                            new FieldDef("nadcapExpiry", "Expiration date", true)),
                    null, List.of()),
            new DocDef("nda", "Signed NDA", true, List.of(), null, List.of())
    );

    public static final List<DocDef> DOCS_FOREIGN = List.of(
            new DocDef("coi", "Certificate of incorporation", false,
                    List.of(new FieldDef("foreignRegNo", "Registration number")), null, List.of()),
            new DocDef("taxreg", "Tax registration certificate", false,
                    List.of(new FieldDef("foreignTaxId", "Tax ID / registration number")), null, List.of()),
            new DocDef("chq", "Bank letter or voided cheque", true,
                    List.of(new FieldDef("benName", "Account name as printed"),
                            new FieldDef("foreignAcctNo", "Account / IBAN number"),
                            new FieldDef("swift", "SWIFT / BIC code")),
                    null, List.of()),
            new DocDef("iso", "ISO 9001 certificate", false,
                    List.of(new FieldDef("isoNo", "Certificate number"),
                            new FieldDef("isoBody", "Certifying body"),
                            new FieldDef("isoExpiry", "Valid to", true)),
                    null, List.of()),
            new DocDef("iso14001", "ISO 14001 certificate", false,
                    List.of(new FieldDef("iso14001No", "Certificate number"),
                            new FieldDef("iso14001Body", "Certifying body"),
                            new FieldDef("iso14001Expiry", "Valid to", true)),
                    null, List.of()),
            new DocDef("iso45001", "ISO 45001 certificate", false,
                    List.of(new FieldDef("iso45001No", "Certificate number"),
                            new FieldDef("iso45001Body", "Certifying body"),
                            new FieldDef("iso45001Expiry", "Valid to", true)),
                    null, List.of()),
            new DocDef("iso27001", "ISO 27001 certificate", false,
                    List.of(new FieldDef("iso27001No", "Certificate number"),
                            new FieldDef("iso27001Body", "Certifying body"),
                            new FieldDef("iso27001Expiry", "Valid to", true)),
                    null, List.of()),
            new DocDef("as", "AS9100D certificate", false,
                    List.of(new FieldDef("asNo", "Certificate number"),
                            new FieldDef("asBody", "Certifying body"),
                            new FieldDef("asExpiry", "Valid to", true)),
                    null, List.of()),
            new DocDef("nadcap", "NADCAP certificate", false,
                    List.of(new FieldDef("nadcapNo", "Certificate number"),
                            new FieldDef("nadcapExpiry", "Expiration date", true)),
                    null, List.of()),
            new DocDef("nda", "Signed NDA", true, List.of(), null, List.of())
    );

    /** The document list for a given vendorType ("DOMESTIC"/"FOREIGN", null treated as domestic). */
    public static List<DocDef> docsFor(String vendorType) {
        return "FOREIGN".equals(vendorType) ? DOCS_FOREIGN : DOCS_DOMESTIC;
    }

    private static final Map<String, DocDef> BY_ID_DOMESTIC = index(DOCS_DOMESTIC);
    private static final Map<String, DocDef> BY_ID_FOREIGN = index(DOCS_FOREIGN);

    private static Map<String, DocDef> index(List<DocDef> docs) {
        return docs.stream().collect(java.util.stream.Collectors.toMap(DocDef::id, d -> d));
    }

    /** Looks up docType in the given vendorType's list specifically - matters for "coi"/"chq",
     *  which exist in both lists with different fields/verifyKind. */
    public static DocDef byId(String vendorType, String docType) {
        DocDef d = ("FOREIGN".equals(vendorType) ? BY_ID_FOREIGN : BY_ID_DOMESTIC).get(docType);
        if (d == null) throw new IllegalArgumentException("Unknown document type: " + docType);
        return d;
    }

    private SupplierDocumentConfig() {}
}
