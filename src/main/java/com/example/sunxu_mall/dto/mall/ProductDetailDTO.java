package com.example.sunxu_mall.dto.mall;

import com.example.sunxu_mall.entity.mall.AttributeValueEntity;
import com.example.sunxu_mall.entity.mall.ProductEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Product Detail DTO")
public class ProductDetailDTO extends ProductEntity {
    
    @Schema(description = "SPU Attribute List")
    private List<AttributeValueEntity> spuAttributeEntityList;

    @Schema(description = "SKU Attribute List")
    private List<AttributeValueEntity> skuAttributeEntityList;

    @Schema(description = "Cover Images")
    private List<String> cover;

    @Schema(description = "Swiper Images")
    private List<String> swiper;

    @Schema(description = "Product Detail (HTML/Rich Text)")
    private String detail;
}
