package com.example.sunxu_mall.controller.mall;

import com.example.sunxu_mall.convert.mall.ProductStructMapper;
import com.example.sunxu_mall.dto.mall.ProductQueryDTO;
import com.example.sunxu_mall.entity.mall.ProductEntity;
import com.example.sunxu_mall.model.ResponsePageEntity;
import com.example.sunxu_mall.service.mall.ProductService;
import com.example.sunxu_mall.vo.mall.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/19 20:21
 * @description
 */

@Tag(name = "商品操作", description = "商品接口")
@RestController
@RequestMapping("/v1/product")
public class ProductController {

    private final ProductService productService;
    private final ProductStructMapper productStructMapper;


    public ProductController(ProductService productService, ProductStructMapper productStructMapper) {
        this.productService = productService;
        this.productStructMapper = productStructMapper;
    }

    /**
     * 通过 id查询商品信息
     *
     * @param id 系统 ID
     * @return 商品信息
     */
    @Operation(summary = "通过id查询商品信息", description = "通过id查询商品信息")
    @GetMapping("/findById")
    public ProductEntity findById(Long id) {
        return productService.findById(id);
    }

    /**
     * 根据条件查询商品列表
     *
     * @param productQueryDTO 条件
     * @return 商品列表
     */
    @Operation(summary = "根据条件查询商品列表", description = "根据条件查询商品列表")
    @PostMapping("/searchByPage")
    public ResponsePageEntity<ProductVO> searchByPage(@RequestBody ProductQueryDTO productQueryDTO) {
        ResponsePageEntity<ProductEntity> pageEntity = productService.searchByPage(productQueryDTO);
        List<ProductVO> voList = productStructMapper.toVOList(pageEntity.getList());
        return new ResponsePageEntity<>(pageEntity.getPageNum(), pageEntity.getPageSize(), pageEntity.getTotal(), voList);
    }

}
