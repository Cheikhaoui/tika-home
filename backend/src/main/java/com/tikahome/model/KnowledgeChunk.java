package com.tikahome.model;

/** One retrievable piece of the knowledge base: a "## section" of a knowledge/*.md file. */
public record KnowledgeChunk(String source, String title, String text) {

    /** What gets embedded and shown to Claude: the title gives the short text its context. */
    public String content() {
        return title + "\n" + text;
    }
}
