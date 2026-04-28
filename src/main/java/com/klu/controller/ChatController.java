package com.klu.controller;

import com.klu.dto.ChatContactDto;
import com.klu.dto.ChatMessageDto;
import com.klu.dto.ChatSendRequestDto;
import com.klu.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat", description = "Global and personal chat for authenticated users")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/contacts")
    @Operation(summary = "Contacts available for personal chat")
    public List<ChatContactDto> getContacts() {
        return chatService.getContacts();
    }

    @GetMapping("/global")
    @Operation(summary = "All global chat messages")
    public List<ChatMessageDto> getGlobalMessages() {
        return chatService.getGlobalMessages();
    }

    @PostMapping("/global")
    @Operation(summary = "Send a global chat message")
    public ChatMessageDto sendGlobalMessage(@Valid @RequestBody ChatSendRequestDto dto) {
        return chatService.sendGlobalMessage(dto.getContent());
    }

    @GetMapping("/private/{otherUserId}")
    @Operation(summary = "Get private conversation with another user")
    public List<ChatMessageDto> getPrivateConversation(@PathVariable Long otherUserId) {
        return chatService.getPrivateConversation(otherUserId);
    }

    @PostMapping("/private")
    @Operation(summary = "Send a private message to another user")
    public ChatMessageDto sendPrivateMessage(@Valid @RequestBody ChatSendRequestDto dto) {
        return chatService.sendPrivateMessage(dto.getRecipientId(), dto.getContent());
    }
}
