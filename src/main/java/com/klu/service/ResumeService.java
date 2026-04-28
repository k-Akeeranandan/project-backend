package com.klu.service;

import com.klu.entity.User;
import com.klu.exception.ApiException;
import com.klu.exception.ResourceNotFoundException;
import com.klu.repo.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ResumeService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx");

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload-dir:uploads/resumes}")
    private String uploadDir;

    public void uploadResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("No file uploaded", HttpStatus.BAD_REQUEST);
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new ApiException("Invalid filename", HttpStatus.BAD_REQUEST);
        }

        String ext = extension(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ApiException("Only PDF or Word documents (.pdf, .doc, .docx) are allowed", HttpStatus.BAD_REQUEST);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new ApiException("Could not create upload directory", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (user.getResumeStoredFileName() != null) {
            try {
                Files.deleteIfExists(root.resolve(user.getResumeStoredFileName()));
            } catch (IOException ignored) {
                // best-effort cleanup
            }
        }

        String storedName = UUID.randomUUID() + "." + ext;
        Path target = root.resolve(storedName);
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new ApiException("Failed to save resume", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = guessContentType(ext);
        }

        user.setResumeStoredFileName(storedName);
        user.setResumeOriginalFileName(original);
        user.setResumeContentType(contentType);
        userRepository.save(user);
    }

    public Resource loadResumeResource(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getResumeStoredFileName() == null) {
            throw new ApiException("No resume on file for this user", HttpStatus.NOT_FOUND);
        }
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = root.resolve(user.getResumeStoredFileName()).normalize();
        if (!filePath.startsWith(root)) {
            throw new ApiException("Invalid file path", HttpStatus.BAD_REQUEST);
        }
        if (!Files.exists(filePath)) {
            throw new ApiException("Resume file missing on server", HttpStatus.NOT_FOUND);
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException("Resume file not readable", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return resource;
        } catch (IOException e) {
            throw new ApiException("Could not read resume", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String getResumeContentType(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getResumeContentType() != null) {
            return user.getResumeContentType();
        }
        if (user.getResumeOriginalFileName() != null) {
            return guessContentType(extension(user.getResumeOriginalFileName()));
        }
        return "application/octet-stream";
    }

    public String getResumeOriginalFileName(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getResumeOriginalFileName() != null ? user.getResumeOriginalFileName() : "resume";
    }

    private static String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) {
            return "";
        }
        return filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static String guessContentType(String ext) {
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }
}
