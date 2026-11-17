package com.example.multimedia.file_upload_api.service;

import com.example.multimedia.file_upload_api.entity.SuperAdmin;
import com.example.multimedia.file_upload_api.entity.UserDetail;
import com.example.multimedia.file_upload_api.repository.SuperAdminRepository;
import com.example.multimedia.file_upload_api.repository.UserDetailRepository;
import com.example.multimedia.file_upload_api.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * Get the current logged-in SuperAdmin
     * @return SuperAdmin entity
     * @throws RuntimeException if no admin is logged in or not found
     */
    public SuperAdmin getCurrentSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        String email = authentication.getName();
        
        // Try to find SuperAdmin first
        return superAdminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("SuperAdmin not found with email: " + email));
    }

    /**
     * Get the current logged-in SuperAdmin ID
     * @return SuperAdmin ID
     * @throws RuntimeException if no admin is logged in or not found
     */
    public Long getCurrentSuperAdminId() {
        return getCurrentSuperAdmin().getSuperAdminId();
    }

    /**
     * Get the current logged-in user (could be SuperAdmin or regular user)
     * @return UserDetail entity
     * @throws RuntimeException if no user is logged in or not found
     */
    public UserDetail getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        String email = authentication.getName();
        
        // Try to find regular user first
        return userDetailRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Get the current logged-in user ID
     * @return User ID
     * @throws RuntimeException if no user is logged in or not found
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * The tenant (super_admin_id) the current principal belongs to, whether they ARE a
     * SuperAdmin or are a regular UserDetail (e.g. an "approver") linked to one. Unlike
     * {@link #getCurrentSuperAdminId()}, this never throws for a non-SuperAdmin login.
     */
    public Long getOwningSuperAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        String email = authentication.getName();
        return superAdminRepository.findByEmail(email)
                .map(SuperAdmin::getSuperAdminId)
                .orElseGet(() -> userDetailRepository.findByEmail(email)
                        .map(u -> u.getSuperAdmin().getSuperAdminId())
                        .orElseThrow(() -> new RuntimeException("No account found with email: " + email)));
    }

    public record PrincipalIdentity(Long id, String email) {}

    /** The current principal's own id + email, whether they are a SuperAdmin or a UserDetail -
     *  for anything (like recording who took an approval action) that just needs "who is this,
     *  regardless of account type" rather than their tenant. */
    public PrincipalIdentity getCurrentPrincipalIdentity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        String email = authentication.getName();
        return superAdminRepository.findByEmail(email)
                .map(a -> new PrincipalIdentity(a.getSuperAdminId(), a.getEmail()))
                .orElseGet(() -> userDetailRepository.findByEmail(email)
                        .map(u -> new PrincipalIdentity(u.getUserId(), u.getEmail()))
                        .orElseThrow(() -> new RuntimeException("No account found with email: " + email)));
    }

    /**
     * Check if the current user is a SuperAdmin
     * @return true if SuperAdmin, false otherwise
     */
    public boolean isCurrentUserSuperAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }

            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> "super_admin".equals(authority.getAuthority()));
        } catch (Exception e) {
            return false;
        }
    }
} 