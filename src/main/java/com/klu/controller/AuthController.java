package com.klu.controller;

import com.klu.config.JwtUtil;
import com.klu.dto.LoginDto;
import com.klu.dto.LoginResponseDto;
import com.klu.dto.ForgotPasswordRequestDto;
import com.klu.dto.ResetPasswordRequestDto;
import com.klu.dto.UserRequestDto;
import com.klu.dto.UserResponseDto;
import com.klu.dto.VerifyOtpRequestDto;
import com.klu.entity.User;
import com.klu.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Register and login — returns JWT for Bearer auth")
public class AuthController {

    @Autowired
    private AuthService service;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account (role USER)")
    public UserResponseDto register(@RequestBody UserRequestDto dto) {
        return service.register(dto);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT access token plus user profile")
    public LoginResponseDto login(@RequestBody LoginDto dto) {
        User user = service.login(dto.getEmail(), dto.getPassword());
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return new LoginResponseDto(token, service.toUserResponseDto(user));
    }

    @PostMapping("/forgot-password/send-otp")
    @Operation(summary = "Send OTP to email for forgot password")
    public Map<String, String> sendForgotPasswordOtp(@Valid @RequestBody ForgotPasswordRequestDto dto) {
        service.sendForgotPasswordOtp(dto.getEmail());
        return Map.of("message", "OTP sent to your email");
    }

    @PostMapping("/forgot-password/verify-otp")
    @Operation(summary = "Verify OTP before allowing password reset")
    public Map<String, String> verifyForgotPasswordOtp(@Valid @RequestBody VerifyOtpRequestDto dto) {
        service.verifyForgotPasswordOtp(dto.getEmail(), dto.getOtp());
        return Map.of("message", "OTP verified successfully");
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Reset password after OTP verification")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequestDto dto) {
        service.resetPasswordWithOtp(dto.getEmail(), dto.getOtp(), dto.getNewPassword());
        return Map.of("message", "Password reset successful");
    }
}
