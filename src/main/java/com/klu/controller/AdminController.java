package com.klu.controller;

import com.klu.dto.BoothDto;
import com.klu.dto.EventDto;
import com.klu.dto.UserResponseDto;
import com.klu.service.BoothService;
import com.klu.service.EventService;
import com.klu.service.AuthService;
import com.klu.service.ResumeService;
import com.klu.service.UserApprovalService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Event and booth management, registrations, resumes (ADMIN role)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @Autowired
    private BoothService boothService;

    @Autowired
    private EventService eventService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private UserApprovalService userApprovalService;

    @PostMapping("/users/{userId}/approve")
    @Operation(summary = "Approve a pending user after reviewing their profile/resume; notifies user by email if mail is enabled")
    public UserResponseDto approveUser(@PathVariable Long userId) {
        return userApprovalService.approve(userId);
    }

    @PostMapping("/users/{userId}/reject")
    @Operation(summary = "Reject a pending user; notifies user by email if mail is enabled")
    public UserResponseDto rejectUser(@PathVariable Long userId) {
        return userApprovalService.reject(userId);
    }

    @GetMapping("/users/{userId}/resume")
    @Operation(summary = "Stream or download a user's resume")
    public ResponseEntity<Resource> getUserResume(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "inline") String disposition) {
        Resource resource = resumeService.loadResumeResource(userId);
        String contentType = resumeService.getResumeContentType(userId);
        String filename = resumeService.getResumeOriginalFileName(userId);

        ContentDisposition cd = ContentDisposition
                .builder("attachment".equalsIgnoreCase(disposition) ? "attachment" : "inline")
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(cd);
        headers.setContentType(MediaType.parseMediaType(contentType));

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @PostMapping("/booth")
    @Operation(summary = "Create a booth for an event")
    public BoothDto createBooth(@RequestBody BoothDto dto){
        return boothService.create(dto);
    }

    @GetMapping("/booths")
    @Operation(summary = "List all booths")
    public List<BoothDto> getBooths(){
        return boothService.getAll();
    }

    @PostMapping("/event")
    @Operation(summary = "Create an event")
    public EventDto createEvent(@RequestBody EventDto dto){
        return eventService.create(dto);
    }

    @GetMapping("/events")
    @Operation(summary = "List all events")
    public List<EventDto> getEvents(){
        return eventService.getAll();
    }

    @GetMapping("/registrations")
    @Operation(summary = "All registered users")
    public List<UserResponseDto> getAllRegistrations(){
        return authService.getAllUsers();
    }

    @GetMapping("/booth/{boothId}/applicants")
    @Operation(summary = "Applicants for a booth")
    public List<UserResponseDto> getBoothApplicants(@PathVariable Long boothId){
        return boothService.getBoothApplicants(boothId);
    }

    @DeleteMapping("/booth/{boothId}")
    @Operation(summary = "Delete a booth")
    public Map<String, String> deleteBooth(@PathVariable Long boothId){
        boothService.deleteBooth(boothId);
        return Map.of("message", "Booth deleted successfully");
    }

    @DeleteMapping("/event/{eventId}")
    @Operation(summary = "Delete an event")
    public Map<String, String> deleteEvent(@PathVariable Long eventId){
        eventService.deleteEvent(eventId);
        return Map.of("message", "Event deleted successfully");
    }
}