package cn.orionsec.ops.controller;

import cn.orionsec.ops.annotation.RestWrapper;
import cn.orionsec.ops.request.AiChatRequest;
import cn.orionsec.ops.service.AiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Api(tags = "AI助手")
@RestController
@RestWrapper
@RequestMapping("/orion/api/ai")
public class AiController {

    @Resource
    private AiService aiService;

    @PostMapping("/chat")
    @ApiOperation(value = "AI对话")
    public Map<String, String> chat(@Valid @RequestBody AiChatRequest request) {
        log.info("AI chat request: {}", request);
        String content = aiService.chat(request);
        Map<String, String> result = new HashMap<>();
        result.put("content", content);
        return result;
    }
}