package com.tikahome.service;

import com.tikahome.model.KnowledgeChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * At startup, makes the vector store match the knowledge files (classpath:knowledge/*.md).
 * Embedding is the slow part, so it only re-indexes when the content actually changed (content hash).
 */
@Component
public class KnowledgeBaseIndexer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseIndexer.class);
    static final String KIND = "knowledge";

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbc;

    public KnowledgeBaseIndexer(VectorStore vectorStore, JdbcTemplate jdbc) {
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        reindexIfChanged();
    }

    /** @return true if the store was rebuilt */
    public synchronized boolean reindexIfChanged() throws IOException {
        List<KnowledgeChunk> chunks = loadChunks();
        String hash = hash(chunks);
        String indexedHash = jdbc.query(
                "select metadata->>'hash' from vector_store where metadata->>'kind' = ? limit 1",
                rs -> rs.next() ? rs.getString(1) : null, KIND);
        if (hash.equals(indexedHash)) {
            log.info("Knowledge base up to date ({} chunks)", chunks.size());
            return false;
        }
        jdbc.update("delete from vector_store where metadata->>'kind' = ?", KIND);
        vectorStore.add(chunks.stream()
                .map(c -> Document.builder()
                        .text(c.content())
                        .metadata(Map.of("kind", KIND, "source", c.source(), "title", c.title(), "hash", hash))
                        .build())
                .toList());
        log.info("Knowledge base indexed: {} chunks", chunks.size());
        return true;
    }

    static List<KnowledgeChunk> loadChunks() throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:knowledge/*.md");
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (Resource file : files) {
            String markdown = file.getContentAsString(StandardCharsets.UTF_8);
            chunks.addAll(KnowledgeChunker.chunk(Objects.requireNonNull(file.getFilename()), markdown));
        }
        chunks.sort((a, b) -> (a.source() + a.title()).compareTo(b.source() + b.title())); // stable hash
        return chunks;
    }

    private static String hash(List<KnowledgeChunk> chunks) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            chunks.forEach(c -> digest.update((c.source() + "\0" + c.content() + "\0").getBytes(StandardCharsets.UTF_8)));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
