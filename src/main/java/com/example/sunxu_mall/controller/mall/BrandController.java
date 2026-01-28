package com.example.sunxu_mall.controller.mall;

import com.example.sunxu_mall.entity.mall.MallBrandEntity;
import com.example.sunxu_mall.entity.mall.MallBrandEntityExample;
import com.example.sunxu_mall.mapper.mall.MallBrandEntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "品牌管理", description = "商品品牌接口")
@RestController
@RequestMapping("/v1/brand")
public class BrandController {

    private final MallBrandEntityMapper brandMapper;

    public BrandController(MallBrandEntityMapper brandMapper) {
        this.brandMapper = brandMapper;
    }

    @Operation(summary = "获取所有品牌", description = "获取所有未删除的品牌列表")
    @GetMapping("/all")
    public List<MallBrandEntity> all() {
        MallBrandEntityExample example = new MallBrandEntityExample();
        example.createCriteria().andIsDelEqualTo(false);
        return brandMapper.selectByExample(example);
    }
}
