package com.tikahome.service;

import com.tikahome.config.AssistantProperties;
import com.tikahome.model.KnowledgeChunk;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/** The "R" of RAG: finds the knowledge sections closest in meaning to a guest's question. */
@Service
public class KnowledgeRetriever {

    private final VectorStore vectorStore;
    private final AssistantProperties properties;

    public KnowledgeRetriever(VectorStore vectorStore, AssistantProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public List<KnowledgeChunk> retrieve(String question) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                        .query(question)
                        .topK(properties.retrievalTopK())
                        .similarityThreshold(properties.retrievalMinSimilarity())
                        .filterExpression("kind == '" + KnowledgeBaseIndexer.KIND + "'")
                        .build())
                .stream()
                .map(d -> {
                    String title = String.valueOf(d.getMetadata().get("title"));
                    String text = d.getText() == null ? "" : d.getText();
                    // Stored text is "title\ntext" (see KnowledgeChunk.content()): keep the body only.
                    String body = text.startsWith(title + "\n") ? text.substring(title.length() + 1) : text;
                    return new KnowledgeChunk(String.valueOf(d.getMetadata().get("source")), title, body);
                })
                .toList();
    }
}
