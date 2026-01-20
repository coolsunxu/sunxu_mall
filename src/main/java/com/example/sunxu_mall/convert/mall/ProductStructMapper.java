package com.example.sunxu_mall.convert.mall;

import com.example.sunxu_mall.entity.mall.ProductEntity;
import com.example.sunxu_mall.vo.mall.ProductVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductStructMapper {

    ProductVO toVO(ProductEntity entity);

    List<ProductVO> toVOList(List<ProductEntity> entityList);
}
