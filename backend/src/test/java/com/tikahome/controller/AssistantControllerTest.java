package com.tikahome.controller;

import com.tikahome.WebMvcSecurityTestConfig;
import com.tikahome.exception.AssistantUnavailableException;
import com.tikahome.service.AssistantChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web layer only: validation and errors raised before streaming starts. Streaming itself: AssistantChatIT. */
@WebMvcTest(AssistantController.class)
@Import(WebMvcSecurityTestConfig.class)
class AssistantControllerTest {

    static final String CHAT = "/api/properties/6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b/assistant/chat";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AssistantChatService chat;

    @MockitoBean(name = "assistantExecutor")
    ExecutorService executor;

    @Test
    void invalidMessageReturns400() throws Exception {
        mvc.perform(post(CHAT).contentType(MediaType.APPLICATION_JSON).content("""
                        {"message":"%s","language":"de"}""".formatted("x".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.message").exists())
                .andExpect(jsonPath("$.errors.language").exists());
        verifyNoInteractions(chat);
    }

    @Test
    void notConfiguredReturns503WithReason() throws Exception {
        when(chat.accept(any(), any(), any(), any(), any())).thenThrow(new AssistantUnavailableException(
                AssistantUnavailableException.Reason.NOT_CONFIGURED, "The assistant is not configured"));

        mvc.perform(post(CHAT).contentType(MediaType.APPLICATION_JSON).content("""
                        {"message":"Hello","language":"fr"}"""))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.reason").value("NOT_CONFIGURED"));
        verifyNoInteractions(executor);
    }
}
