package com.example.sunxu_mall.dto.common;

import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用任务创建请求 DTO
 * @author sunxu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonTaskRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 幂等去重 Key（由切面基于 userId + bizType + paramJson 生成）
     * 用于任务创建去重：同一 dedupKey 只创建一次任务
     */
    private String dedupKey;

    /**
     * 任务参数指纹（userId + bizType + paramJson 的摘要）
     */
    private String fingerprint;

    /**
     * 业务类型
     */
    private ExcelBizTypeEnum bizType;

    /**
     * 请求参数 JSON
     */
    private String paramJson;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户姓名
     */
    private String userName;
}
