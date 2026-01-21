package com.example.sunxu_mall.service.mall;

import com.example.sunxu_mall.dto.mall.ProductDetailDTO;
import com.example.sunxu_mall.dto.mall.ProductQueryDTO;
import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.entity.mall.*;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.mall.*;
import com.example.sunxu_mall.service.BaseService;
import com.example.sunxu_mall.util.BeanCopyUtils;
import com.example.sunxu_mall.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.sunxu_mall.errorcode.ErrorCode.PARAMETER_MISSING;
import static com.example.sunxu_mall.errorcode.ErrorCode.USER_NOT_EXIST;

@Slf4j
@Service
public class ProductService extends BaseService<ProductEntity, ProductQueryDTO> {

    private final ProductEntityMapper productMapper;
    private final ProductGroupAttributeEntityMapper productGroupAttributeMapper;
    private final ProductAttributeEntityMapper productAttributeMapper;
    private final AttributeValueEntityMapper attributeValueMapper;
    private final CommonPhotoEntityMapper commonPhotoMapper;

    public ProductService(
            ProductEntityMapper productMapper,
            ProductGroupAttributeEntityMapper productGroupAttributeMapper,
            ProductAttributeEntityMapper productAttributeMapper,
            AttributeValueEntityMapper attributeValueMapper,
            CommonPhotoEntityMapper commonPhotoMapper
    ) {
        this.productMapper = productMapper;
        this.productGroupAttributeMapper = productGroupAttributeMapper;
        this.productAttributeMapper = productAttributeMapper;
        this.attributeValueMapper = attributeValueMapper;
        this.commonPhotoMapper = commonPhotoMapper;
    }

    /**
     * Get product detail by ID
     *
     * @param id Product ID
     * @return ProductDetailDTO
     */
    public ProductDetailDTO findById(Long id) {
        ProductEntity productEntity = productMapper.selectByPrimaryKey(id);
        if (Objects.isNull(productEntity)) {
            return null;
        }

        ProductDetailDTO productDetailDTO = new ProductDetailDTO();
        BeanCopyUtils.copyNonNullProperties(productEntity, productDetailDTO);

        fillSpuAttributeValue(productDetailDTO);
        fillSkuAttributeValue(productDetailDTO);
        fillPhoto(productDetailDTO);
        // fillDetail(productDetailDTO); // Mongo functionality pending

        return productDetailDTO;
    }

    @Override
    protected List<ProductEntity> selectListWithLimit(ProductQueryDTO queryDTO, int limit) {
        return productMapper.selectListWithLimit(
                queryDTO.getName(),
                queryDTO.getModel(),
                queryDTO.getCategoryId(),
                queryDTO.getBrandId(),
                queryDTO.getProductGroupId(),
                limit
        );
    }

    @Override
    protected List<ProductEntity> selectListByCursorWithLimit(ProductQueryDTO queryDTO, Long cursorId, int limit) {
        return productMapper.selectByCursorWithLimit(
                queryDTO.getName(),
                queryDTO.getModel(),
                queryDTO.getCategoryId(),
                queryDTO.getBrandId(),
                queryDTO.getProductGroupId(),
                cursorId,
                limit
        );
    }

    @Override
    protected Long extractEntityId(ProductEntity entity) {
        return Objects.isNull(entity) ? null : entity.getId();
    }


    /**
     * Create a new product
     *
     * @param productEntity
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(ProductEntity productEntity) {
        if (Objects.isNull(productEntity)) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "Product entity cannot be null");
        }

        JwtUserEntity currentUser = SecurityUtil.getUserInfo();
        productEntity.setCreateUserId(currentUser.getId());
        productEntity.setCreateUserName(currentUser.getUsername());
        productEntity.setUpdateUserId(currentUser.getId());
        productEntity.setUpdateUserName(currentUser.getUsername());
        productEntity.setCreateTime(LocalDateTime.now());
        productEntity.setUpdateTime(LocalDateTime.now());
        productEntity.setIsDel(false);
        productEntity.setVersion(1);

        productMapper.insertSelective(productEntity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public int update(ProductEntity productEntity) {
        if (Objects.isNull(productEntity) || Objects.isNull(productEntity.getId())) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "Product ID cannot be null");
        }

        Long productId = productEntity.getId();
        ProductEntity current = productMapper.selectByPrimaryKey(productId);
        if (Objects.isNull(current)) {
            throw new BusinessException(USER_NOT_EXIST.getCode(), "Product not found");
        }

        Integer oldVersion = current.getVersion();
        BeanCopyUtils.copyNonNullProperties(productEntity, current);

        JwtUserEntity currentUser = SecurityUtil.getUserInfo();
        current.setUpdateUserId(currentUser.getId());
        current.setUpdateUserName(currentUser.getUsername());
        current.setUpdateTime(LocalDateTime.now());
        current.setVersion(oldVersion);

        int rows = productMapper.updateProductInfoWithVersion(current);
        if (rows == 0) {
            log.warn("Optimistic lock failure for productId {}", productId);
            throw new OptimisticLockingFailureException("Product data has been modified, please retry");
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        ProductEntityExample example = new ProductEntityExample();
        example.createCriteria().andIdIn(ids);

        ProductEntity update = new ProductEntity();
        update.setIsDel(true);
        update.setUpdateTime(LocalDateTime.now());

        JwtUserEntity currentUser = SecurityUtil.getUserInfo();
        update.setUpdateUserId(currentUser.getId());
        update.setUpdateUserName(currentUser.getUsername());

        return productMapper.updateByExampleSelective(update, example);
    }

    private void fillSpuAttributeValue(ProductDetailDTO productDetailDTO) {
        if (Objects.isNull(productDetailDTO.getProductGroupId())) {
            return;
        }

        ProductGroupAttributeEntityExample example = new ProductGroupAttributeEntityExample();
        example.createCriteria().andProductGroupIdEqualTo(productDetailDTO.getProductGroupId());
        List<ProductGroupAttributeEntity> groupAttributes = productGroupAttributeMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(groupAttributes)) {
            return;
        }

        List<Long> attributeValueIds = groupAttributes.stream()
                .map(ProductGroupAttributeEntity::getAttributeValueId)
                .distinct()
                .collect(Collectors.toList());

        productDetailDTO.setSpuAttributeEntityList(getAttributeValues(attributeValueIds));
    }


    private void fillSkuAttributeValue(ProductDetailDTO productDetailDTO) {
        ProductAttributeEntityExample example = new ProductAttributeEntityExample();
        example.createCriteria().andProductIdEqualTo(productDetailDTO.getId());
        List<ProductAttributeEntity> productAttributes = productAttributeMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(productAttributes)) {
            return;
        }

        List<Long> attributeValueIds = productAttributes.stream()
                .map(ProductAttributeEntity::getAttributeValueId)
                .distinct()
                .collect(Collectors.toList());

        productDetailDTO.setSkuAttributeEntityList(getAttributeValues(attributeValueIds));
    }

    private List<AttributeValueEntity> getAttributeValues(List<Long> attributeValueIds) {
        if (CollectionUtils.isEmpty(attributeValueIds)) {
            return Collections.emptyList();
        }

        AttributeValueEntityExample example = new AttributeValueEntityExample();
        example.createCriteria().andIdIn(attributeValueIds);
        List<AttributeValueEntity> attributeValues = attributeValueMapper.selectByExample(example);

        // Note: Attribute name filling logic removed as AttributeEntity is missing
        return attributeValues;
    }

    private void fillPhoto(ProductDetailDTO productDetailDTO) {
        // Assuming CommonPhotoEntity's photoGroupId maps to Product ID
        CommonPhotoEntityExample example = new CommonPhotoEntityExample();
        example.createCriteria().andPhotoGroupIdEqualTo(productDetailDTO.getId());
        List<CommonPhotoEntity> photos = commonPhotoMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(photos)) {
            return;
        }

        // Logic to separate cover and swiper if needed. 
        // Since CommonPhotoEntity doesn't have "type" field visible in the read output, 
        // we might just put all in swiper, or assume cover is already in ProductEntity.coverUrl.

        List<String> photoUrls = photos.stream()
                .map(CommonPhotoEntity::getUrl)
                .collect(Collectors.toList());

        productDetailDTO.setSwiper(photoUrls);

        if (StringUtils.isBlank(productDetailDTO.getCoverUrl()) && !photoUrls.isEmpty()) {
            productDetailDTO.setCover(Collections.singletonList(photoUrls.get(0)));
        } else if (StringUtils.isNotBlank(productDetailDTO.getCoverUrl())) {
            productDetailDTO.setCover(Collections.singletonList(productDetailDTO.getCoverUrl()));
        }
    }

    /*
    private void fillDetail(ProductDetailDTO productDetailDTO) {
        // Mongo implementation placeholder
        // Query query = new Query(Criteria.where("productId").is(productDetailDTO.getId()));
        // List<ProductDetailEntity> productDetailEntities = mongoTemplate.find(query, ProductDetailEntity.class);
        // ...
    }
    */
}
