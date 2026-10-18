package com.tikahome.service;

import com.tikahome.model.KnowledgeChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits a knowledge Markdown file into one chunk per "## heading" section. Small, self-contained
 * sections retrieve better than big pages, and the embedding model only reads ~128 tokens per text.
 */
public final class KnowledgeChunker {

    private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    private KnowledgeChunker() {
    }

    public static List<KnowledgeChunk> chunk(String source, String markdown) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String title = null;
        StringBuilder text = new StringBuilder();
        for (String line : HTML_COMMENT.matcher(markdown).replaceAll("").split("\\R")) {
            if (line.startsWith("## ")) {
                add(chunks, source, title, text);
                title = line.substring(3).strip();
                text.setLength(0);
            } else if (title != null && !line.startsWith("# ")) {
                text.append(line.strip()).append(' ');
            }
        }
        add(chunks, source, title, text);
        return chunks;
    }

    private static void add(List<KnowledgeChunk> chunks, String source, String title, StringBuilder text) {
        String body = text.toString().replaceAll("\\s+", " ").strip();
        if (title != null && !body.isEmpty()) {
            chunks.add(new KnowledgeChunk(source, title, body));
        }
    }
}
