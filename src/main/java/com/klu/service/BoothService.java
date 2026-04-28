package com.klu.service;

import com.klu.dto.BoothDto;
import com.klu.dto.BoothApplicationRequestDto;
import com.klu.dto.BoothApplicationResponseDto;
import com.klu.dto.UserResponseDto;
import com.klu.entity.BoothApplication;
import com.klu.entity.Booth;
import com.klu.entity.Event;
import com.klu.entity.User;
import com.klu.exception.ApiException;
import com.klu.exception.ResourceNotFoundException;
import com.klu.repo.BoothRepository;
import com.klu.repo.BoothApplicationRepository;
import com.klu.repo.EventRepository;
import com.klu.repo.UserRepository;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BoothService {

    @Autowired
    private BoothRepository boothRepo;

    @Autowired
    private EventRepository eventRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private BoothApplicationRepository boothApplicationRepo;

    @Autowired
    private ModelMapper mapper;

    public BoothDto create(BoothDto dto){

        Booth booth = mapper.map(dto, Booth.class);

        // 🔥 SET EVENT RELATION
        Event event = eventRepo.findById(dto.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        booth.setEvent(event);

        // 🔥 SET APPLICANTS (optional)
        if(dto.getApplicantIds() != null){
            List<User> users = userRepo.findAllById(dto.getApplicantIds());
            booth.setApplicants(users);
        }

        Booth saved = boothRepo.save(booth);

        BoothDto response = mapper.map(saved, BoothDto.class);
        response.setEventId(saved.getEvent().getId());

        return response;
    }

    public List<BoothDto> getAll(){
        return boothRepo.findAll()
                .stream()
                .map(booth -> {
                    BoothDto dto = mapper.map(booth, BoothDto.class);

                    dto.setEventId(booth.getEvent().getId());

                    if(booth.getApplicants() != null){
                        dto.setApplicantIds(
                                booth.getApplicants()
                                        .stream()
                                        .map(User::getId)
                                        .collect(Collectors.toList())
                        );
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void applyToBooth(Long boothId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booth booth = boothRepo.findById(boothId)
                .orElseThrow(() -> new ResourceNotFoundException("Booth not found"));

        if (booth.getApplicants() == null) {
            booth.setApplicants(new ArrayList<>());
        }

        if (booth.getApplicants().contains(user)) {
            throw new ApiException("Already applied to this booth", HttpStatus.CONFLICT);
        }

        booth.getApplicants().add(user);
        boothRepo.save(booth);
    }

    public BoothApplicationResponseDto submitApplication(Long boothId, BoothApplicationRequestDto dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booth booth = boothRepo.findById(boothId)
                .orElseThrow(() -> new ResourceNotFoundException("Booth not found"));

        if (boothApplicationRepo.existsByBoothIdAndUserId(boothId, user.getId())) {
            throw new ApiException("You have already submitted an application for this booth", HttpStatus.CONFLICT);
        }

        BoothApplication application = new BoothApplication();
        application.setBooth(booth);
        application.setUser(user);
        application.setFullName(dto.getFullName());
        application.setEmail(dto.getEmail());
        application.setPhoneNumber(dto.getPhoneNumber());
        application.setCurrentProfession(dto.getCurrentProfession());
        application.setEducationLevel(dto.getEducationLevel());
        application.setCollegeName(dto.getCollegeName());
        application.setGraduationYear(dto.getGraduationYear());
        application.setSkills(dto.getSkills());
        application.setCoverLetter(dto.getCoverLetter());
        application.setCreatedAt(java.time.Instant.now());

        if (booth.getApplicants() == null) {
            booth.setApplicants(new ArrayList<>());
        }
        if (!booth.getApplicants().contains(user)) {
            booth.getApplicants().add(user);
            boothRepo.save(booth);
        }

        return toApplicationResponse(boothApplicationRepo.save(application));
    }

    public BoothApplicationResponseDto getMyApplicationForBooth(Long boothId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BoothApplication application = boothApplicationRepo.findByBoothIdAndUserId(boothId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return toApplicationResponse(application);
    }

    private BoothApplicationResponseDto toApplicationResponse(BoothApplication application) {
        BoothApplicationResponseDto dto = new BoothApplicationResponseDto();
        dto.setId(application.getId());
        dto.setBoothId(application.getBooth().getId());
        dto.setUserId(application.getUser().getId());
        dto.setFullName(application.getFullName());
        dto.setEmail(application.getEmail());
        dto.setPhoneNumber(application.getPhoneNumber());
        dto.setCurrentProfession(application.getCurrentProfession());
        dto.setEducationLevel(application.getEducationLevel());
        dto.setCollegeName(application.getCollegeName());
        dto.setGraduationYear(application.getGraduationYear());
        dto.setSkills(application.getSkills());
        dto.setCoverLetter(application.getCoverLetter());
        dto.setCreatedAt(application.getCreatedAt());
        return dto;
    }

    public List<BoothDto> getMyApplications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return boothRepo.findAll()
                .stream()
                .filter(booth -> booth.getApplicants() != null && booth.getApplicants().contains(user))
                .map(booth -> {
                    BoothDto dto = mapper.map(booth, BoothDto.class);
                    dto.setEventId(booth.getEvent().getId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<UserResponseDto> getBoothApplicants(Long boothId) {
        Booth booth = boothRepo.findById(boothId)
                .orElseThrow(() -> new ResourceNotFoundException("Booth not found"));

        if (booth.getApplicants() == null) {
            return List.of();
        }

        return booth.getApplicants()
                .stream()
                .map(user -> mapper.map(user, UserResponseDto.class))
                .collect(Collectors.toList());
    }

    public void deleteBooth(Long boothId) {
        Booth booth = boothRepo.findById(boothId)
                .orElseThrow(() -> new ResourceNotFoundException("Booth not found"));
        boothRepo.delete(booth);
    }
}