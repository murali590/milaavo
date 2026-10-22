package com.milaavo.auth.service.impl;

import com.milaavo.auth.dto.LoginRequest;
import com.milaavo.auth.dto.LoginResponse;
import com.milaavo.auth.dto.RegisterRequest;
import com.milaavo.auth.entity.User;
import com.milaavo.auth.enums.UserStatus;
import com.milaavo.auth.repository.UserRepository;
import com.milaavo.auth.service.JwtService;
import com.milaavo.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.milaavo.auth.dto.RegisterResponse;
import com.milaavo.exception.DuplicateMobileNumberException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;

        @Override
        @Transactional
        public RegisterResponse register(RegisterRequest request) {
                String mobileNumber = request.getMobileNumber().trim();

                if (userRepository.existsByMobileNumber(mobileNumber)) {
                        throw new DuplicateMobileNumberException(
                                        "An account with this mobile number already exists");
                }

                User user = new User();
                user.setName(request.getName().trim());
                user.setMobileNumber(mobileNumber);
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                user.setMobileVerified(false);
                user.setStatus(UserStatus.PENDING_VERIFICATION);

                User savedUser = userRepository.save(user);

                return RegisterResponse.builder()
                                .userId(savedUser.getId())
                                .name(savedUser.getName())
                                .mobileNumber(savedUser.getMobileNumber())
                                .mobileVerified(savedUser.isMobileVerified())
                                .status(savedUser.getStatus())
                                .createdAt(savedUser.getCreatedAt())
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public LoginResponse login(LoginRequest request) {
                String mobileNumber = request.getMobileNumber().trim();

                User user = userRepository.findByMobileNumber(mobileNumber)
                                .orElseThrow(() -> new BadCredentialsException(
                                                "Invalid mobile number or password"));

                if (!passwordEncoder.matches(
                                request.getPassword(), user.getPasswordHash())) {
                        throw new BadCredentialsException(
                                        "Invalid mobile number or password");
                }

                if (user.getStatus() != UserStatus.ACTIVE
                                || !user.isMobileVerified()) {
                        throw new AccessDeniedException(
                                        "Account is not active or mobile is not verified");
                }

                return LoginResponse.builder()
                                .userId(user.getId())
                                .name(user.getName())
                                .mobileNumber(user.getMobileNumber())
                                .mobileVerified(user.isMobileVerified())
                                .status(user.getStatus().name())
                                .accessToken(jwtService.generateAccessToken(user.getId()))
                                .tokenType("Bearer")
                                .expiresIn(jwtService.getExpirySeconds())
                                .build();
        }

}
