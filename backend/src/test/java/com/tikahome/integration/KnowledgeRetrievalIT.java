package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import com.tikahome.model.KnowledgeChunk;
import com.tikahome.service.KnowledgeBaseIndexer;
import com.tikahome.service.KnowledgeRetriever;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real RAG retrieval: local multilingual embeddings + pgvector. Questions in French, English and Arabic
 * must find the right (English) knowledge section among the top results.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class KnowledgeRetrievalIT {

    @Autowired KnowledgeRetriever retriever;
    @Autowired KnowledgeBaseIndexer indexer;

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
            Is the swimming pool heated?                 | Pool
            Est-ce que la piscine est chauffée ?         | Pool
            Où peut-on garer la voiture ?                | Parking
            هل يوجد واي فاي في المنزل؟                    | Comfort and connectivity
            À quelle heure peut-on arriver ?             | Check-in and check-out times
            Can I bring my dog?                          | Pets
            Peut-on fumer dans la maison ?               | Smoking
            How do I get from the airport to the house?  | Airport transfer
            هل الفطور متوفر؟                              | Breakfast
            """)
    void findsTheRightSectionInAnyLanguage(String question, String expectedTitle) {
        assertThat(retriever.retrieve(question))
                .extracting(KnowledgeChunk::title)
                .as("top sections for: %s", question)
                .contains(expectedTitle);
    }

    @Test
    void unrelatedQuestionsRetrieveLittleOrNothing() {
        assertThat(retriever.retrieve("What is the best programming language for quantum computers?")).hasSizeLessThan(2);
    }

    @Test
    void indexIsNotRebuiltWhenNothingChanged() throws Exception {
        assertThat(indexer.reindexIfChanged()).isFalse();
    }
}
