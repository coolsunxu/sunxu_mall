package com.example.sunxu_mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

@Data
@Schema(description = "Base Page Query")
public class BasePageQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Page number", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "Page size", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "Whether to query total count", example = "true")
    private Boolean needTotal = true;

    /**
     * 旧版单向游标分页字段（当前对外主接口已统一为 bidirectional cursor + cursorToken）
     * 保留仅用于兼容历史代码/入参，不建议继续使用。
     */
    @Schema(description = "[Deprecated] Cursor id for legacy keyset pagination", example = "100", deprecated = true)
    private Long cursorId;

    /**
     * 旧版单向游标分页字段（当前对外主接口已统一为 bidirectional cursor + cursorToken）
     * 保留仅用于兼容历史代码/入参，不建议继续使用。
     */
    @Schema(description = "[Deprecated] Cursor direction for legacy keyset pagination: NEXT/PREV", example = "NEXT", deprecated = true)
    private String cursorDirection = "NEXT"; // NEXT, PREV

    @Schema(description = "Cursor state token for bidirectional pagination (required for jumping within visited pages)", example = "eyJwYWdlTnVtIjozLCJwYWdlU2l6ZSI6MTAsImxhc3RJZCI6MTAwfQ==")
    private String cursorToken; // Base64编码的游标状态
}
