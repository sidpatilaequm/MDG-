package com.example.multimedia.file_upload_api.service.email;

import com.example.multimedia.file_upload_api.entity.email.EmailFooter;
import com.example.multimedia.file_upload_api.entity.email.EmailTemplate;
import com.example.multimedia.file_upload_api.repository.email.EmailFooterRepository;
import com.example.multimedia.file_upload_api.repository.email.EmailTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * {{placeholder}} substitution into one branded template - the single rendering mechanism for
 * both applicant-facing and approver-notification emails (unlike WorkFlow's two separate
 * ones). Templates and the global footer are editable rows (EmailTemplate/EmailFooter),
 * seeded by V9's migration.
 */
@Service
public class EmailTemplateRenderingService {

    private static final Pattern LEFTOVER_PLACEHOLDER = Pattern.compile("\\{\\{[^}]+}}");

    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailFooterRepository emailFooterRepository;

    public EmailTemplateRenderingService(EmailTemplateRepository emailTemplateRepository,
                                          EmailFooterRepository emailFooterRepository) {
        this.emailTemplateRepository = emailTemplateRepository;
        this.emailFooterRepository = emailFooterRepository;
    }

    public record RenderedEmail(String subject, String html, String plainText) {}

    public RenderedEmail render(String templateKey, Map<String, Object> variables) {
        EmailTemplate template = emailTemplateRepository.findByTemplateKeyAndIsActiveTrue(templateKey)
                .orElseThrow(() -> new IllegalStateException("No active email template for key: " + templateKey));

        EmailFooter footer = emailFooterRepository.findById(1L).orElse(null);
        String footerHtml = footer != null && footer.getHtmlFooter() != null ? footer.getHtmlFooter() : "";
        String footerPlain = footer != null && footer.getPlainTextFooter() != null ? footer.getPlainTextFooter() : "";

        String subject = substitute(template.getSubject(), variables);
        String html = substitute(template.getHtmlBody(), variables) + footerHtml;
        String plainText = substitute(template.getPlainTextBody(), variables) + "\n\n" + footerPlain;
        return new RenderedEmail(subject, html, plainText);
    }

    private String substitute(String text, Map<String, Object> variables) {
        if (text == null) return "";
        String result = text;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            result = result.replace("{{" + entry.getKey() + "}}", value);
        }
        // Any placeholder left un-filled renders as empty rather than leaking "{{...}}" to the reader.
        return LEFTOVER_PLACEHOLDER.matcher(result).replaceAll("");
    }
}
