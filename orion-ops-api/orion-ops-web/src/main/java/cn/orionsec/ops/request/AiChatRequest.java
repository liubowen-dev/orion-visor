package cn.orionsec.ops.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * AI 聊天请求
 *
 * @author orion
 * @date 2026-03-23
 */
@Data
public class AiChatRequest {

    /**
     * 消息列表
     */
    @NotNull(message = "messages cannot be null")
    private List<Message> messages;

    /**
     * 当前页面路由名称（如 machineMonitor、machineList）
     */
    private String currentRouteName;

    /**
     * 当前页面路径（如 /machine/monitor/metrics/1）
     */
    private String currentPath;

    /**
     * 当前选中的机器 ID
     */
    private Long currentMachineId;

    /**
     * 当前选中的应用 ID
     */
    private Long currentAppId;

    /**
     * 消息
     */
    @Data
    public static class Message {

        /**
         * 角色: system / user / assistant
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;
    }
}