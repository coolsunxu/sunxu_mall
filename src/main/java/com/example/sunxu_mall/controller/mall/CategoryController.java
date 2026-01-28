package com.example.sunxu_mall.controller.mall;

import com.example.sunxu_mall.entity.mall.MallCategoryEntity;
import com.example.sunxu_mall.entity.mall.MallCategoryEntityExample;
import com.example.sunxu_mall.mapper.mall.MallCategoryEntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "分类管理", description = "商品分类接口")
@RestController
@RequestMapping("/v1/category")
public class CategoryController {

    private final MallCategoryEntityMapper categoryMapper;

    public CategoryController(MallCategoryEntityMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Operation(summary = "获取分类树", description = "获取所有分类并组装成树形结构")
    @GetMapping("/tree")
    public List<Map<String, Object>> tree() {
        MallCategoryEntityExample example = new MallCategoryEntityExample();
        example.createCriteria().andIsDelEqualTo(false);
        List<MallCategoryEntity> all = categoryMapper.selectByExample(example);

        List<Map<String, Object>> result = new ArrayList<>();
        Map<Long, Map<String, Object>> map = new HashMap<>();

        // Convert to map
        for (MallCategoryEntity c : all) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", c.getId());
            node.put("name", c.getName());
            node.put("parentId", c.getParentId());
            node.put("children", new ArrayList<Map<String, Object>>());
            map.put(c.getId(), node);
        }

        // Build tree
        for (MallCategoryEntity c : all) {
            Map<String, Object> node = map.get(c.getId());
            if (c.getParentId() == null || c.getParentId() == 0) {
                result.add(node);
            } else {
                Map<String, Object> parent = map.get(c.getParentId());
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                } else {
                    result.add(node);
                }
            }
        }

        return result;
    }
}
