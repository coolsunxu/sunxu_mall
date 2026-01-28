package com.example.sunxu_mall.controller.mall;

import com.example.sunxu_mall.entity.mall.MallUnitEntity;
import com.example.sunxu_mall.entity.mall.MallUnitEntityExample;
import com.example.sunxu_mall.mapper.mall.MallUnitEntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "单位管理", description = "商品单位接口")
@RestController
@RequestMapping("/v1/unit")
public class UnitController {

    private final MallUnitEntityMapper unitMapper;

    public UnitController(MallUnitEntityMapper unitMapper) {
        this.unitMapper = unitMapper;
    }

    @Operation(summary = "获取所有单位", description = "获取所有未删除的单位列表")
    @GetMapping("/all")
    public List<MallUnitEntity> all() {
        MallUnitEntityExample example = new MallUnitEntityExample();
        example.createCriteria().andIsDelEqualTo(false);
        return unitMapper.selectByExample(example);
    }
}
