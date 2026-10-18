package com.tikahome.model;

/** A previous message of the conversation, replayed as plain text (role: "user" or "assistant"). */
public record ChatTurn(String role, String text) {
}
