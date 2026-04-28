package com.klu.service;

import com.klu.dto.ChatContactDto;
import com.klu.dto.ChatMessageDto;
import com.klu.entity.ChatMessage;
import com.klu.entity.User;
import com.klu.exception.ApiException;
import com.klu.exception.ResourceNotFoundException;
import com.klu.repo.ChatMessageRepository;
import com.klu.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getGlobalMessages() {
        return chatMessageRepository.findGlobalMessages().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getPrivateConversation(Long otherUserId) {
        User currentUser = getCurrentUserEntity();
        ensureUserExists(otherUserId);
        return chatMessageRepository.findPrivateConversation(currentUser.getId(), otherUserId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ChatMessageDto sendGlobalMessage(String content) {
        User currentUser = getCurrentUserEntity();
        ChatMessage message = new ChatMessage();
        message.setSender(currentUser);
        message.setRecipient(null);
        message.setScope("GLOBAL");
        message.setContent(normalizeContent(content));
        message.setCreatedAt(Instant.now());
        return toDto(chatMessageRepository.save(message));
    }

    @Transactional
    public ChatMessageDto sendPrivateMessage(Long recipientId, String content) {
        if (recipientId == null) {
            throw new ApiException("Recipient is required for private chat", HttpStatus.BAD_REQUEST);
        }
        User currentUser = getCurrentUserEntity();
        if (currentUser.getId().equals(recipientId)) {
            throw new ApiException("You cannot send private messages to yourself", HttpStatus.BAD_REQUEST);
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

        ChatMessage message = new ChatMessage();
        message.setSender(currentUser);
        message.setRecipient(recipient);
        message.setScope("PRIVATE");
        message.setContent(normalizeContent(content));
        message.setCreatedAt(Instant.now());
        return toDto(chatMessageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<ChatContactDto> getContacts() {
        User currentUser = getCurrentUserEntity();
        return userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .sorted(Comparator
                        .comparing((User user) -> !"ADMIN".equals(user.getRole()))
                        .thenComparing(user -> safe(user.getName()))
                        .thenComparing(user -> safe(user.getEmail())))
                .map(this::toContactDto)
                .toList();
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
    }

    private User getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new ApiException("Message cannot be empty", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private ChatContactDto toContactDto(User user) {
        ChatContactDto dto = new ChatContactDto();
        dto.setId(user.getId());
        dto.setName(safe(user.getName()).isBlank() ? user.getEmail() : user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }

    private ChatMessageDto toDto(ChatMessage message) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(message.getId());
        dto.setScope(message.getScope());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(safe(message.getSender().getName()).isBlank() ? message.getSender().getEmail() : message.getSender().getName());
        dto.setSenderRole(message.getSender().getRole());
        if (message.getRecipient() != null) {
            dto.setRecipientId(message.getRecipient().getId());
            dto.setRecipientName(safe(message.getRecipient().getName()).isBlank() ? message.getRecipient().getEmail() : message.getRecipient().getName());
        }
        return dto;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
