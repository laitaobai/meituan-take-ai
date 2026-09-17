package com.sky.taobai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class DocumentCompressor {

    //文本压缩 ,语义压缩
    @Bean
    public QueryTransformer compressionQueryTransformer(ChatClient.Builder chatClientBuilder) {
        return new CompressionQueryTransformer(chatClientBuilder, null);
    }



}
