package com.example.multimedia.file_upload_api.security;

import com.example.multimedia.file_upload_api.entity.SuperAdmin;
import com.example.multimedia.file_upload_api.entity.UserAuthentication;
import com.example.multimedia.file_upload_api.entity.UserDetail;
import com.example.multimedia.file_upload_api.repository.SuperAdminRepository;
import com.example.multimedia.file_upload_api.repository.UserAuthenticationRepository;
import com.example.multimedia.file_upload_api.repository.UserDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private UserAuthenticationRepository userAuthenticationRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // First try to find a SuperAdmin
        Optional<SuperAdmin> superAdminOpt = superAdminRepository.findByEmail(email);
        if (superAdminOpt.isPresent()) {
            SuperAdmin superAdmin = superAdminOpt.get();
            return new SuperAdminUserDetails(superAdmin);
        }

        // If not a SuperAdmin, try to find a regular user
        Optional<UserDetail> userOpt = userDetailRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            UserDetail user = userOpt.get();
            List<GrantedAuthority> authorities = userAuthenticationRepository
                    .findAllActiveByUserId(user.getUserId())
                    .stream()
                    .map(UserAuthentication::getAuthKey)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            return new User(user.getEmail(), user.getPassword(), authorities);
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }

    public Long getUserIdByEmail(String email) {
        // First try to find a SuperAdmin
        Optional<SuperAdmin> superAdminOpt = superAdminRepository.findByEmail(email);
        if (superAdminOpt.isPresent()) {
            return superAdminOpt.get().getSuperAdminId();
        }

        // If not a SuperAdmin, try to find a regular user
        UserDetail user = userDetailRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return user.getUserId();
    }
}