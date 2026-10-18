package com.tikahome.service;

import com.tikahome.model.KnowledgeChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkerTest {

    @Test
    void oneChunkPerSectionIgnoringTitleAndComments() {
        String markdown = """
                # The house
                <!-- note for the owner,
                     not for guests -->

                ## Pool
                The plunge pool is
                not heated.

                ## Wi-Fi
                Fibre everywhere.
                """;

        List<KnowledgeChunk> chunks = KnowledgeChunker.chunk("house.md", markdown);

        assertThat(chunks).containsExactly(
                new KnowledgeChunk("house.md", "Pool", "The plunge pool is not heated."),
                new KnowledgeChunk("house.md", "Wi-Fi", "Fibre everywhere."));
        assertThat(chunks.getFirst().content()).isEqualTo("Pool\nThe plunge pool is not heated.");
    }

    @Test
    void emptySectionsAreSkipped() {
        assertThat(KnowledgeChunker.chunk("x.md", "## Empty\n\n## Full\nText")).extracting(KnowledgeChunk::title)
                .containsExactly("Full");
    }

    @Test
    void realKnowledgeFilesHaveShortSections() throws Exception {
        // The embedding model reads ~128 tokens: keep every section short (≈ 90 words).
        assertThat(KnowledgeBaseIndexer.loadChunks())
                .isNotEmpty()
                .allSatisfy(c -> assertThat(c.content().split("\\s+").length).as(c.title()).isLessThanOrEqualTo(95));
    }
}
