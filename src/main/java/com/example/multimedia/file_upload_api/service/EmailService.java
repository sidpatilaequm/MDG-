package com.example.multimedia.file_upload_api.service;

import com.example.multimedia.file_upload_api.entity.UserDetail;
import com.example.multimedia.file_upload_api.repository.UserDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Optional;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserDetailRepository userDetailRepository;

    public void sendSimpleEmailToUserId(Long userId, String subject, String body) {
        Optional<UserDetail> userOpt = userDetailRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        String toEmail = userOpt.get().getEmail();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("your_email@gmail.com");
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        System.out.println("Mail Sent Successfully to " + toEmail);
    }

    /** Sends a rendered HTML email (with a plain-text fallback part) to an arbitrary address -
     *  used by the supplier-onboarding/workflow email templates, which aren't tied to a
     *  UserDetail row (a draft applicant may not have an account yet). Logs and swallows send
     *  failures rather than throwing, so a flaky mail server never blocks the business
     *  operation (draft save, submit, approval) that triggered the email. */
    public void sendHtmlEmail(String toEmail, String subject, String html, String plainText) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, html);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            logger.warn("Failed to send email '{}' to {}", subject, toEmail, e);
        }
    }
}
