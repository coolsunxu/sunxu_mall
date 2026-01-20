package com.example.sunxu_mall.dto.mall;

import com.example.sunxu_mall.dto.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Product Query DTO")
public class ProductQueryDTO extends BasePageQuery {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Product Name")
    private String name;

    @Schema(description = "Model")
    private String model;

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Brand ID")
    private Long brandId;

    @Schema(description = "Product Group ID")
    private Long productGroupId;
}
