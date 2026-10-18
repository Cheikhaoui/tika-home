package com.tikahome.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RAG building blocks (Spring AI), declared explicitly instead of via auto-configuring starters:
 * - a LOCAL embedding model (ONNX, runs inside the JVM: free, no API key, data stays on our server).
 *   paraphrase-multilingual-MiniLM-L12-v2 maps FR / EN / AR text into the same 384-dimension space,
 *   so an Arabic question finds an English passage.
 * - a pgvector store; its table and HNSW index are created by Flyway (V10), not by Spring AI.
 */
@Configuration
public class RagConfig {

    static final int EMBEDDING_DIMENSIONS = 384;

    @Bean(destroyMethod = "close")
    TransformersEmbeddingModel embeddingModel(AssistantProperties properties) throws Exception {
        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        model.setModelResource(properties.embeddingModelUri());
        model.setTokenizerResource(properties.embeddingTokenizerUri());
        model.setResourceCacheDirectory(properties.modelCacheDirectory());
        model.afterPropertiesSet(); // downloads (first time only) and loads the ONNX model
        return model;
    }

    @Bean
    VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(EMBEDDING_DIMENSIONS)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(false)
                .build();
    }
}
