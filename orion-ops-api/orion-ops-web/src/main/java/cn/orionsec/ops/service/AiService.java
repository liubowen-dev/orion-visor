package cn.orionsec.ops.service;

import cn.orionsec.ops.request.AiChatRequest;

public interface AiService {

    /**
     * AI 对话
     * @param request 请求参数
     * @return AI 回复内容
     */
    String chat(AiChatRequest request);
}