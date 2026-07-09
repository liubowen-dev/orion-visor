package cn.orionsec.ops.service.impl;

import cn.orionsec.ops.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final String KNOWLEDGE_BASE_PATH = "classpath:knowledge-base/*.txt";

    private List<DocumentChunk> chunks = new ArrayList<>();

    @Override
    public List<String> search(String query, int topK) {
        if (chunks.isEmpty()) {
            log.warn("知识库为空，无法检索");
            return Collections.emptyList();
        }

        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        log.info("RAG 检索查询: {}", query);

        // 运维关键词列表
        String[] techKeywords = {"CPU", "内存", "磁盘", "网络", "重启", "Nginx", "MySQL", "Java",
                "Docker", "Redis", "JVM", "GC", "线程", "死锁", "慢查询", "日志",
                "备份", "恢复", "部署", "构建", "tomcat", "spring", "linux"};

        List<ScoredChunk> scored = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            int score = 0;
            for (String keyword : techKeywords) {
                if (query.contains(keyword) && chunk.content.contains(keyword)) {
                    score += keyword.length();
                }
            }
            // 特殊处理 CPU 查询（用户可能写 CPU、cpu、Cpu 等）
            if ((query.contains("CPU") || query.contains("cpu")) && chunk.content.contains("CPU")) {
                score += 10;
            }
            if (score > 0) {
                scored.add(new ScoredChunk(chunk.content, score));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score, a.score));
        List<String> results = scored.stream()
                .limit(topK)
                .map(sc -> sc.content)
                .collect(Collectors.toList());

        log.info("RAG 检索到 {} 条结果", results.size());
        return results;
    }

    @PostConstruct
    public void init() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(KNOWLEDGE_BASE_PATH);

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                log.info("加载知识库文件: {}", filename);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }

                    String[] paragraphs = content.toString().split("\n\n");
                    for (String paragraph : paragraphs) {
                        String trimmed = paragraph.trim();
                        if (!trimmed.isEmpty()) {
                            chunks.add(new DocumentChunk(trimmed));
                        }
                    }
                }
            }

            log.info("知识库初始化完成，共加载 {} 个文档块", chunks.size());

        } catch (Exception e) {
            log.error("知识库初始化失败", e);
        }
    }

    private static class DocumentChunk {
        String content;
        DocumentChunk(String content) {
            this.content = content;
        }
    }

    private static class ScoredChunk {
        String content;
        int score;
        ScoredChunk(String content, int score) {
            this.content = content;
            this.score = score;
        }
    }
}