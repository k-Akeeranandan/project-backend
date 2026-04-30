package com.klu.service;

import com.klu.dto.UserRequestDto;
import com.klu.dto.UserResponseDto;
import com.klu.entity.AccountStatus;
import com.klu.entity.Booth;
import com.klu.entity.BoothApplication;
import com.klu.entity.User;
import com.klu.exception.ApiException;
import com.klu.exception.ResourceNotFoundException;
import com.klu.repo.BoothApplicationRepository;
import com.klu.repo.UserRepository;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private static final long OTP_VALIDITY_SECONDS = 10 * 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, OtpSession> otpStore = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository repo;

    @Autowired
    private BoothApplicationRepository boothApplicationRepo;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private ModelMapper mapper;

    @Autowired
    private EmailService emailService;

    public UserResponseDto register(UserRequestDto dto) {
        if (repo.existsByEmail(dto.getEmail())) {
            throw new ApiException("Email already exists", HttpStatus.CONFLICT);
        }
        User user = mapper.map(dto, User.class);
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setAccountStatus(AccountStatus.PENDING);

        User saved = repo.save(user);
        return toUserResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public User login(String email, String password) {
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!encoder.matches(password, user.getPassword())) {
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        return user;
    }

    public void sendForgotPasswordOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = repo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException("No account found for this email", HttpStatus.NOT_FOUND));

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plusSeconds(OTP_VALIDITY_SECONDS);
        otpStore.put(normalizedEmail, new OtpSession(otp, expiresAt));

        String subject = "Your password reset OTP";
        String body = "Hello " + (user.getName() != null ? user.getName() : "User") + ",\n\n"
                + "Use this OTP to reset your Virtual Career Fair password: " + otp + "\n"
                + "This OTP is valid for 10 minutes.\n\n"
                + "If you did not request this, you can ignore this email.";
        boolean sent = emailService.sendPlainText(normalizedEmail, subject, body);
        if (!sent) {
            otpStore.remove(normalizedEmail);
            throw new ApiException("Email service is not configured. Please contact admin.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public void verifyForgotPasswordOtp(String email, String otp) {
        OtpSession session = getValidOtpSession(email, otp);
        session.verified = true;
        otpStore.put(normalizeEmail(email), session);
    }

    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new ApiException("No account found for this email", HttpStatus.NOT_FOUND));

        OtpSession session = getValidOtpSession(email, otp);
        if (!session.verified) {
            throw new ApiException("Please verify OTP before resetting password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(encoder.encode(newPassword));
        repo.save(user);
        otpStore.remove(normalizeEmail(email));
    }

    private OtpSession getValidOtpSession(String email, String otp) {
        OtpSession session = otpStore.get(normalizeEmail(email));
        if (session == null) {
            throw new ApiException("OTP not found. Please request a new OTP", HttpStatus.BAD_REQUEST);
        }

        if (Instant.now().isAfter(session.expiresAt)) {
            otpStore.remove(normalizeEmail(email));
            throw new ApiException("OTP expired. Please request a new OTP", HttpStatus.BAD_REQUEST);
        }

        if (!session.otp.equals(otp)) {
            throw new ApiException("Invalid OTP", HttpStatus.BAD_REQUEST);
        }
        return session;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static final class OtpSession {
        private final String otp;
        private final Instant expiresAt;
        private boolean verified;

        private OtpSession(String otp, Instant expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
            this.verified = false;
        }
    }

    /**
     * Maps {@link User} to {@link UserResponseDto}, including booth IDs (ModelMapper skips relation lists).
     */
    @Transactional(readOnly = true)
    public UserResponseDto toUserResponseDto(User user) {
        UserResponseDto dto = mapper.map(user, UserResponseDto.class);
        if (user.getAccountStatus() != null) {
            dto.setAccountStatus(user.getAccountStatus().name());
        } else {
            dto.setAccountStatus(AccountStatus.PENDING.name());
        }
        if (user.getAppliedBooths() != null) {
            dto.setAppliedBoothIds(
                    user.getAppliedBooths().stream().map(Booth::getId).collect(Collectors.toList()));
        } else {
            dto.setAppliedBoothIds(List.of());
        }
        enrichWithLatestApplication(dto, user);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return repo.findAllWithAppliedBooths()
                .stream()
                .filter(user -> "USER".equals(user.getRole()))
                .map(this::toUserResponseDtoActiveEventsOnly)
                .filter(user -> user.getAppliedBoothIds() != null && !user.getAppliedBoothIds().isEmpty())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponseDto(user);
    }

    private UserResponseDto toUserResponseDtoActiveEventsOnly(User user) {
        UserResponseDto dto = mapper.map(user, UserResponseDto.class);
        if (user.getAccountStatus() != null) {
            dto.setAccountStatus(user.getAccountStatus().name());
        } else {
            dto.setAccountStatus(AccountStatus.PENDING.name());
        }
        if (user.getAppliedBooths() != null) {
            dto.setAppliedBoothIds(
                    user.getAppliedBooths().stream()
                            .filter(booth -> booth.getEvent() != null && isEventVisible(booth.getEvent().getDate()))
                            .map(Booth::getId)
                            .collect(Collectors.toList())
            );
        } else {
            dto.setAppliedBoothIds(List.of());
        }
        enrichWithLatestApplication(dto, user);
        return dto;
    }

    @Transactional(readOnly = true)
    private void enrichWithLatestApplication(UserResponseDto dto, User user) {
        if (dto == null || user == null || user.getId() == null) return;

        BoothApplication app = boothApplicationRepo
                .findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElse(null);
        if (app == null) return;

        dto.setPhoneNumber(app.getPhoneNumber());
        dto.setCurrentProfession(app.getCurrentProfession());
        dto.setEducationLevel(app.getEducationLevel());
        dto.setCollegeName(app.getCollegeName());
        dto.setGraduationYear(app.getGraduationYear());
        dto.setSkills(app.getSkills());
        dto.setCoverLetter(app.getCoverLetter());
    }

    private boolean isEventVisible(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return true;
        }
        Instant eventInstant = parseEventInstant(rawDate);
        return eventInstant == null || !eventInstant.isBefore(Instant.now());
    }

    private Instant parseEventInstant(String rawDate) {
        String trimmed = rawDate == null ? "" : rawDate.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // try other accepted formats below
        }
        try {
            return LocalDateTime.parse(trimmed).atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ignored) {
            // try date-only format below
        }
        try {
            return LocalDate.parse(trimmed).atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
