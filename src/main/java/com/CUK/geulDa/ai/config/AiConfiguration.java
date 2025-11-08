package com.CUK.geulDa.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@Slf4j
public class AiConfiguration {

    @Value("${geulda.vector-store.path:vector-store.json}")
    private String vectorStorePath;

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        File vectorFile = new File(vectorStorePath);

        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        if (vectorFile.exists()) {
            vectorStore.load(vectorFile);
            log.info("✅ 기존 벡터 스토어 로드 완료: {} ({}KB)",
                    vectorStorePath, vectorFile.length() / 1024);
        } else {
            log.info("🆕 새로운 벡터 스토어 생성 (파일 없음: {})", vectorStorePath);
        }

        return vectorStore;
    }
}
