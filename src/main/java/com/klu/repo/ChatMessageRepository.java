package com.klu.repo;

import com.klu.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            SELECT m FROM ChatMessage m
            JOIN FETCH m.sender s
            LEFT JOIN FETCH m.recipient r
            WHERE m.scope = 'GLOBAL'
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessage> findGlobalMessages();

    @Query("""
            SELECT m FROM ChatMessage m
            JOIN FETCH m.sender s
            LEFT JOIN FETCH m.recipient r
            WHERE m.scope = 'PRIVATE'
              AND ((s.id = :userId AND r.id = :otherUserId)
                OR (s.id = :otherUserId AND r.id = :userId))
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessage> findPrivateConversation(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
