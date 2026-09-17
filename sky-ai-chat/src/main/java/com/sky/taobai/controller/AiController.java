package com.sky.taobai.controller;


import com.sky.taobai.Service.AiToolService;
import com.sky.taobai.entiy.AiDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class AiController {

    @Autowired
    SimpleVectorStore simpleVectorStore;

    private  final ChatClient chatClient;     //先定义一个ai聊天助手，后面再进行属性的添加，完善

    //conversationId
    private static final String conversationId = "user";


    public AiController(OpenAiChatModel openAiChatModel
                        ,ChatMemory chatMemory,
                        AiToolService aiToolService,
                        @Value("classpath:/file/prompt.st") Resource resource){
        //创建拥有对话记忆的拦截器
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor
                .builder(chatMemory).build();

        //对ai进行属性添加
         this.chatClient = ChatClient.builder(openAiChatModel)
                .defaultSystem(resource)
                .defaultAdvisors(memoryAdvisor)
                 .defaultAdvisors(new SimpleLoggerAdvisor()) //设置对话拦截,查看日志中ai的输出
                .defaultTools(aiToolService)
                .build();

    }

    @RequestMapping(value = "/chat",produces = "text/steam;charset=UTF-8")
    public Flux<String> chat(String msg){
//        String msg = aiDto.getMsg();

        //1.设置向量数据库检索的一些前置条件
        SearchRequest query = SearchRequest.builder().query(msg).build();

        //将用户的提示词向量化，并到向量数据库检索，然后得到对应的文档
        List<Document> documents = simpleVectorStore.similaritySearch(query);

        if (documents.isEmpty() || documents.size() == 0){
            System.out.println("检索的文档为空。请查看向量数据库是否有值");
        }


        //将从数据库检索出来的文档转化成字符串，便于LLM进行读取和分析
        String context = documents.stream().map(Document::getText)
                .collect(Collectors.joining("\n"));

        //把检索出来的上下文档和用户的提示词放在一起
        String template = """
        #参考资料：
        {0}
        
        #我的问题是：
        {1}
        """;
        String format = MessageFormat.format(template, context, msg);

        //LLM最终进行处理
        Flux<String> content = chatClient.prompt()
                .advisors(advisorSpec ->
                        advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(format)
                .stream().content();

        return content;
    }





}
