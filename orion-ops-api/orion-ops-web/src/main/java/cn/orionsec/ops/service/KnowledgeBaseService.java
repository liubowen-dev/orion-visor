package cn.orionsec.ops.service;

import java.util.List;

public interface KnowledgeBaseService {

    /**
     * 搜索知识库
     * @param query 用户问题
     * @param topK 返回最相关的 K 条结果
     * @return 相关文档内容列表
     */
    List<String> search(String query, int topK);
}