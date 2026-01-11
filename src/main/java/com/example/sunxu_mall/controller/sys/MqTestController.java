package com.example.sunxu_mall.controller.sys;

import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.service.mq.IMqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "MQ测试接口")
@RestController
@RequestMapping("/sys/mq")
public class MqTestController {

    private final IMqService mqService;

    public MqTestController(IMqService mqService) {
        this.mqService = mqService;
    }

    @Operation(summary = "发送MQ消息")
    @PostMapping("/send")
    public String send(@RequestParam String eventType, 
                            @RequestParam(required = false) @Parameter(description = "指定Topic，不填使用默认配置") String topic,
                            @RequestBody Map<String, Object> content) {
        MqMessage message = buildMessage(eventType, content);
        if (topic != null && !topic.isEmpty()) {
            mqService.send(topic, message);
            return "MQ消息发送成功(Topic: " + topic + ")";
        } else {
            mqService.send(message);
            return "MQ消息发送成功(默认Topic)";
        }
    }

    private MqMessage buildMessage(String eventType, Map<String, Object> content) {
        return MqMessage.builder()
                .eventType(eventType)
                .businessKey(UUID.randomUUID().toString())
                .content(content)
                .build();
    }
}
