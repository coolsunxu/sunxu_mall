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
}
