package com.sky.taobai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Memory {

    @Bean //添加短期记忆
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository){

        return MessageWindowChatMemory.builder().maxMessages(10)
                .chatMemoryRepository(chatMemoryRepository).build();
    }

    @Bean //使用spring自带的向量数据库
    SimpleVectorStore simpleVectorStore(OpenAiEmbeddingModel openAiEmbeddingModel){
        return SimpleVectorStore.builder(openAiEmbeddingModel).build();
    }
}
