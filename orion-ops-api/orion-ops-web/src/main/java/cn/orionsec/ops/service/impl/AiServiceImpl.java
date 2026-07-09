package cn.orionsec.ops.service.impl;

import cn.orionsec.ops.request.AiChatRequest;
import cn.orionsec.ops.service.AiService;
import cn.orionsec.ops.service.KnowledgeBaseService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private static final String DEEPSEEK_URL = "https://api.deepseek.com/v1/chat/completions";

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    public String chat(AiChatRequest request) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("DeepSeek API Key not configured");
            return "DeepSeek API Key 未配置，请联系管理员设置。";
        }

        try {
            // 获取用户最后一条消息（用于 RAG 检索）
            String userQuery = getLastUserMessage(request.getMessages());

            // ========== RAG 检索：从知识库找相关文档 ==========
            List<String> relevantDocs = new ArrayList<>();
            if (knowledgeBaseService != null && userQuery != null && !userQuery.isEmpty()) {
                try {
                    log.info("knowledgeBaseService is null: {}, userQuery: {}", knowledgeBaseService == null, userQuery);
                    relevantDocs = knowledgeBaseService.search(userQuery, 3);
                    log.info("RAG 检索结果: {}", relevantDocs);
                    if (!relevantDocs.isEmpty()) {
                        log.info("RAG 检索到 {} 条相关文档", relevantDocs.size());
                    }
                } catch (Exception e) {
                    log.warn("RAG 检索失败", e);
                }
            }

            // ========== 构建系统提示词（场景化 + RAG） ==========
            String systemPrompt = buildSystemPrompt(request, relevantDocs);

            // 构建消息列表：系统提示词放在最前面
            List<AiChatRequest.Message> finalMessages = new ArrayList<>();

            AiChatRequest.Message systemMessage = new AiChatRequest.Message();
            systemMessage.setRole("system");
            systemMessage.setContent(systemPrompt);
            finalMessages.add(systemMessage);

            // 添加用户的历史消息（跳过原有的 system 消息）
            for (AiChatRequest.Message msg : request.getMessages()) {
                if (!"system".equals(msg.getRole())) {
                    finalMessages.add(msg);
                }
            }

            // 调用 DeepSeek API
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("messages", finalMessages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            log.info("Calling DeepSeek API, model: {}, messagesCount: {}", model, finalMessages.size());

            Request httpRequest = new Request.Builder()
                    .url(DEEPSEEK_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(
                            MediaType.parse("application/json"),
                            requestBody.toJSONString()
                    ))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    log.error("DeepSeek API error: {} - {}", response.code(), responseBody);
                    return "AI 服务暂时不可用，请稍后再试。";
                }

                JSONObject jsonResponse = JSON.parseObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");

                if (choices != null && !choices.isEmpty()) {
                    JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                    return message.getString("content");
                }

                return "未能获取到有效回复，请重试。";
            }

        } catch (IOException e) {
            log.error("DeepSeek API call failed", e);
            return "网络请求失败，请检查网络连接。";
        } catch (Exception e) {
            log.error("Unexpected error calling DeepSeek API", e);
            return "服务异常，请稍后再试。";
        }
    }

    /**
     * 获取用户最后一条消息
     */
    private String getLastUserMessage(List<AiChatRequest.Message> messages) {
        if (messages == null) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatRequest.Message msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return null;
    }

    /**
     * 构建系统提示词（场景化 + RAG）
     */
    private String buildSystemPrompt(AiChatRequest request, List<String> relevantDocs) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的运维助手，帮助用户解决运维相关问题。\n\n");

        // ========== 场景化信息 ==========
        prompt.append("【用户当前场景】\n");
        if (request.getCurrentRouteName() != null) {
            prompt.append("- 当前页面：").append(request.getCurrentRouteName()).append("\n");
        }
        if (request.getCurrentPath() != null) {
            prompt.append("- 当前路径：").append(request.getCurrentPath()).append("\n");
        }
        if (request.getCurrentMachineId() != null) {
            prompt.append("- 当前选中的机器 ID：").append(request.getCurrentMachineId()).append("\n");
        }
        if (request.getCurrentAppId() != null) {
            prompt.append("- 当前选中的应用 ID：").append(request.getCurrentAppId()).append("\n");
        }

        // 根据页面类型添加提示
        String routeName = request.getCurrentRouteName();
        if (routeName != null) {
            if (routeName.contains("monitor")) {
                prompt.append("- 提示：用户在监控页面，可以主动询问是否需要查看 CPU、内存、磁盘等监控数据。\n");
            } else if (routeName.contains("machine")) {
                prompt.append("- 提示：用户在机器管理页面，可以询问是否需要连接某台机器或查看机器状态。\n");
            } else if (routeName.contains("build")) {
                prompt.append("- 提示：用户在构建页面，可以询问是否需要帮助配置构建命令或查看构建日志。\n");
            } else if (routeName.contains("release")) {
                prompt.append("- 提示：用户在发布页面，可以询问是否需要帮助发布应用到目标服务器。\n");
            } else if (routeName.contains("scheduler")) {
                prompt.append("- 提示：用户在调度任务页面，可以询问是否需要创建定时任务。\n");
            }
        }
        prompt.append("\n");

        // ========== RAG 检索到的参考资料 ==========
        if (relevantDocs != null && !relevantDocs.isEmpty()) {
            prompt.append("【参考资料】\n");
            for (int i = 0; i < relevantDocs.size(); i++) {
                prompt.append(i + 1).append(". ").append(relevantDocs.get(i)).append("\n");
            }
            prompt.append("\n请优先使用参考资料中的内容来回答用户问题。如果参考资料中没有相关信息，再使用你自己的知识。\n\n");
        }

        prompt.append("回答要求：\n");
        prompt.append("1. 回答要简洁专业\n");
        prompt.append("2. 如果涉及敏感操作（如删除文件、重启服务、修改配置），要提醒用户谨慎执行\n");
        prompt.append("3. 如果用户提供了具体的信息（如机器 ID、应用 ID），尽量结合这些信息回答\n");

        return prompt.toString();
    }
}