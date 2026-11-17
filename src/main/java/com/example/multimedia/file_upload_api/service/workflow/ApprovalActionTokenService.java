package com.example.multimedia.file_upload_api.service.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.ApprovalDecision;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

/**
 * Signs/validates the one-click approve/reject links embedded in approver-notification
 * emails. Deliberately uses its own secret (workflow.action-token.secret), never JwtUtil's
 * login-session secret - a leaked action link must never be usable as a login session, and
 * vice versa.
 */
@Service
public class ApprovalActionTokenService {

    private static final long VALIDITY_MILLIS = 3L * 24 * 60 * 60 * 1000; // 3 days

    private final SecretKey signingKey;

    public ApprovalActionTokenService(@Value("${workflow.action-token.secret}") String secret) {
        // Pad/stretch via SHA-256 so any secret length still yields a valid HS256 key.
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.signingKey = Keys.hmacShaKeyFor(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize approval action token signing key", e);
        }
    }

    public record ActionTokenPayload(Long requestStageId, Long approverId, String approverEmail, ApprovalDecision action) {}

    public String generate(Long requestStageId, Long approverId, String approverEmail, ApprovalDecision action) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(requestStageId))
                .claim("approverId", approverId)
                .claim("approverEmail", approverEmail)
                .claim("action", action.name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + VALIDITY_MILLIS))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Throws if the token is malformed, expired, or tampered with. */
    public ActionTokenPayload parse(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(signingKey).build()
                .parseClaimsJws(token).getBody();
        return new ActionTokenPayload(
                Long.valueOf(claims.getSubject()),
                claims.get("approverId", Long.class),
                claims.get("approverEmail", String.class),
                ApprovalDecision.valueOf(claims.get("action", String.class)));
    }
}
