package com.klu.controller;

import com.klu.dto.BoothDto;
import com.klu.dto.BoothApplicationRequestDto;
import com.klu.dto.BoothApplicationResponseDto;
import com.klu.dto.EventDto;
import com.klu.dto.UserResponseDto;
import com.klu.service.AuthService;
import com.klu.service.BoothService;
import com.klu.service.EventService;
import com.klu.service.ResumeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "Authenticated user profile, events, booths, applications (USER role)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private BoothService boothService;

    @Autowired
    private EventService eventService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ResumeService resumeService;

    @GetMapping("/me")
    @Operation(summary = "Current user profile")
    public UserResponseDto getCurrentUser() {
        return authService.getCurrentUserProfile();
    }

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload resume (multipart file)")
    public Map<String, String> uploadResume(@RequestPart("file") MultipartFile file) {
        resumeService.uploadResume(file);
        return Map.of("message", "Resume uploaded successfully");
    }

    @GetMapping("/booths")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "List all booths (USER)")
    public List<BoothDto> viewBooths(){
        return boothService.getAll();
    }

    @GetMapping("/events")
    @Operation(summary = "List all events (public)")
    public List<EventDto> viewEvents(){
        return eventService.getAll();

    }

    @PostMapping("/apply/{boothId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Apply to a booth (USER)")
    public Map<String, String> applyToBooth(@PathVariable Long boothId){
        boothService.applyToBooth(boothId);
        return Map.of("message", "Successfully applied to booth");
    }

    @PostMapping("/applications/{boothId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Submit full application details for a booth")
    public BoothApplicationResponseDto submitApplication(
            @PathVariable Long boothId,
            @Valid @RequestBody BoothApplicationRequestDto dto) {
        return boothService.submitApplication(boothId, dto);
    }

    @GetMapping("/applications/{boothId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my application details for a booth")
    public BoothApplicationResponseDto getMyApplicationForBooth(@PathVariable Long boothId) {
        return boothService.getMyApplicationForBooth(boothId);
    }

    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Booths the current user applied to")
    public List<BoothDto> getMyApplications(){
        return boothService.getMyApplications();
    }
}