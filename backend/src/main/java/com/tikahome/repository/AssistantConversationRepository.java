package com.tikahome.repository;

import com.tikahome.entity.AssistantConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, UUID> {
}
