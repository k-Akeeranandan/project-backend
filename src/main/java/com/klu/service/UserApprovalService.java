package com.klu.service;

import com.klu.dto.UserResponseDto;
import com.klu.entity.AccountStatus;
import com.klu.entity.User;
import com.klu.exception.ApiException;
import com.klu.exception.ResourceNotFoundException;
import com.klu.repo.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserApprovalService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthService authService;

    @Value("${app.mail.application-name:Virtual Career Fair}")
    private String applicationName;

    @Transactional
    public UserResponseDto approve(Long userId) {
        User user = loadApplicant(userId);
        if (user.getAccountStatus() == AccountStatus.APPROVED) {
            return authService.toUserResponseDto(user);
        }

        user.setAccountStatus(AccountStatus.APPROVED);
        userRepository.save(user);

        boolean sent = emailService.sendPlainText(
                user.getEmail(),
                "Congratulations! You are selected — " + applicationName,
                "Hello " + user.getName() + ",\n\n"
                        + "Congratulations! You have been selected.\n\n"
                        + "Venue / workspace details will be updated shortly. Please keep an eye on your dashboard for updates.\n\n"
                        + "Regards,\n"
                        + applicationName);
        if (!sent) {
            throw new ApiException("User approved, but notification email could not be sent. Please configure mail settings.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return authService.toUserResponseDto(user);
    }

    @Transactional
    public UserResponseDto reject(Long userId) {
        User user = loadApplicant(userId);
        if (user.getAccountStatus() == AccountStatus.REJECTED) {
            return authService.toUserResponseDto(user);
        }

        user.setAccountStatus(AccountStatus.REJECTED);
        userRepository.save(user);

        boolean sent = emailService.sendPlainText(
                user.getEmail(),
                "Update on your application — " + applicationName,
                "Hello " + user.getName() + ",\n\n"
                        + "Thank you for your interest in " + applicationName + ".\n\n"
                        + "After review, we are unable to select your application at this time.\n\n"
                        + "We encourage you to apply again for upcoming opportunities.\n\n"
                        + "Regards,\n"
                        + applicationName);
        if (!sent) {
            throw new ApiException("User rejected, but notification email could not be sent. Please configure mail settings.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return authService.toUserResponseDto(user);
    }

    private User loadApplicant(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!"USER".equals(user.getRole())) {
            throw new ApiException("Only applicant accounts can be approved or rejected", HttpStatus.BAD_REQUEST);
        }
        return user;
    }
}
