package com.example.sunxu_mall.vo.mall;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Product VO")
public class ProductVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Product ID")
    private Long id;

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Product Group ID")
    private Long productGroupId;

    @Schema(description = "Brand ID")
    private Long brandId;

    @Schema(description = "Unit ID")
    private Long unitId;

    @Schema(description = "Product Name")
    private String name;

    @Schema(description = "Model")
    private String model;

    @Schema(description = "Quantity")
    private Integer quantity;

    @Schema(description = "Remain Quantity")
    private Integer remainQuantity;

    @Schema(description = "Price")
    private BigDecimal price;

    @Schema(description = "Cover URL")
    private String coverUrl;

    @Schema(description = "Create Time")
    private LocalDateTime createTime;

    @Schema(description = "Update Time")
    private LocalDateTime updateTime;

    @Schema(description = "Version")
    private Integer version;
}
