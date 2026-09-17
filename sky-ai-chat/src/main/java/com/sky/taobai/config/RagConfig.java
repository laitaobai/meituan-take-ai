package com.sky.taobai.config;

import okhttp3.OkHttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class RagConfig {

    @Bean    //多 Query 扩展
    MultiQueryExpander multiQueryExpander(ChatClient.Builder chatClientBuilder) {
        // 可选：自定义扩展提示词，以更好地控制子查询的生成
        String expansionPrompt = """
                请将以下用户问题扩展为{number}个不同的子查询，每个子查询从不同角度表述，以覆盖更全面的信息。
                子查询之间用换行分隔。
                用户问题：{query}
                扩展的子查询：
                """;
        PromptTemplate promptTemplate = new PromptTemplate(expansionPrompt); //文本中的query指的是用户的问题


        MultiQueryExpander build = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder) // 注入 ChatClient 构建器[reference:9]
                .numberOfQueries(3)                  // 设置生成子查询的数量[reference:10] ，对应提示词中的number，为3
                .promptTemplate(promptTemplate)     // 使用自定义提示词模板[reference:11]
                // .includeOriginal(false)           // 可选：是否包含原始查询[reference:12]
                .build();
        return build;
    }

    @Bean   //多query的优化
    public ChatClient chatClient(ChatClient.Builder builder,
                                 SimpleVectorStore simpleVectorStore,
                                 MultiQueryExpander queryExpander,
                                 QueryTransformer queryTransformer) {
        // 1. 创建一个检索器，例如基于 VectorStore 的检索器
        var retriever = VectorStoreDocumentRetriever.builder().vectorStore(simpleVectorStore).build();

        // 2. 构建一个检索增强的 Advisor
        var ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryExpander(queryExpander) // 注入查询扩展器[reference:18]
                .documentRetriever(retriever)
               .queryTransformers(queryTransformer) // 可选：添加文档转换器，"文本压缩"
                // .documentJoiner(...)      // 可选：添加文档合并器
                .build();

        // 3. 将 Advisor 应用到 ChatClient
        return builder
                .defaultAdvisors(ragAdvisor)
                .build();
    }

    @Bean  //增加 HTTP 客户端的超时时间
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }


}
