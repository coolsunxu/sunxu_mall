package com.example.sunxu_mall.controller.sys;

import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.mq.producer.MessageProducer;
import com.example.sunxu_mall.util.JsonUtil;
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

    private final MessageProducer messageProducer;

    public MqTestController(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @Operation(summary = "发送MQ消息")
    @PostMapping("/send")
    public String send(@RequestParam String eventType, 
                            @RequestParam(required = false) @Parameter(description = "指定Topic，不填使用默认配置") String topic,
                            @RequestBody Map<String, Object> content) {
        MqMessage message = buildMessage(eventType, content);
        String finalTopic = (topic != null && !topic.isEmpty()) ? topic : com.example.sunxu_mall.constant.MQConstant.MALL_COMMON_TASK_TOPIC;
        
        messageProducer.send(finalTopic, "TAG_TEST", message.getBusinessKey(), JsonUtil.toJsonStr(message));
        return "MQ消息发送成功(Topic: " + finalTopic + ")";
    }

    private MqMessage buildMessage(String eventType, Map<String, Object> content) {
        return MqMessage.builder()
                .eventType(eventType)
                .businessKey(UUID.randomUUID().toString())
                .content(content)
                .build();
    }
}
