package com.sky.taobai.controller;


import com.sky.taobai.Service.AiToolService;
import com.sky.taobai.Service.impl.AiToolServiceImpl;
import com.sky.taobai.context.PendingAction;
import com.sky.taobai.context.PendingActionStore;
import com.sky.taobai.utils.Jwt;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class AiController {

    @Autowired
    SimpleVectorStore simpleVectorStore;

    @Autowired
    PendingActionStore pendingActionStore;

    private final ChatClient chatClient;

    public AiController(OpenAiChatModel openAiChatModel
                        ,ChatMemory chatMemory,
                        AiToolService aiToolService,
                        @Value("classpath:/file/prompt.st") Resource resource){
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor
                .builder(chatMemory).build();

         this.chatClient = ChatClient.builder(openAiChatModel)
                .defaultSystem(resource)
                .defaultAdvisors(memoryAdvisor)
                 .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultTools(aiToolService)
                .build();

    }

    @RequestMapping(value = "/chat",produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chat(String msg,
                             @RequestHeader(value = "authentication", required = false) String token){

        String conversationId = resolveConversationId(token);

        SearchRequest query = SearchRequest.builder().query(msg).build();

        List<Document> documents = simpleVectorStore.similaritySearch(query);

        if (documents.isEmpty()){
            System.out.println("检索的文档为空。请查看向量数据库是否有值");
        }

        String context = documents.stream().map(Document::getText)
                .collect(Collectors.joining("\n"));

        String template = """
        #参考资料：
        {0}
        
        #我的问题是：
        {1}
        """;
        String format = MessageFormat.format(template, context, msg);

        Flux<String> content = chatClient.prompt()
                .advisors(advisorSpec ->
                        advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of(AiToolServiceImpl.TOKEN_KEY, token == null ? "" : token))
                .user(format)
                .stream().content();

        return content;
    }

    /**
     * 拉取当前用户待确认的动作，前端据此弹出确认卡片
     */
    @GetMapping("/pending-action")
    public PendingAction pendingAction(@RequestHeader(value = "authentication", required = false) String token) {
        return pendingActionStore.peek(Jwt.parseUserIdOrNull(token));
    }

    /**
     * 用户确认或忽略后清除待确认动作
     */
    @DeleteMapping("/pending-action")
    public void clearPendingAction(@RequestHeader(value = "authentication", required = false) String token) {
        pendingActionStore.remove(Jwt.parseUserIdOrNull(token));
    }

    /**
     * 按用户隔离会话，避免多用户共用同一份对话记忆
     */
    private String resolveConversationId(String token) {
        Long userId = Jwt.parseUserIdOrNull(token);
        return userId == null ? "anonymous" : "user-" + userId;
    }
}
