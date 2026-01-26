package com.example.sunxu_mall.service.mall;

import com.example.sunxu_mall.dto.mall.ProductDetailDTO;
import com.example.sunxu_mall.dto.mall.ProductQueryDTO;
import com.example.sunxu_mall.dto.mall.UpdateProductAttributeDTO;
import com.example.sunxu_mall.dto.mall.UpdateProductDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.sunxu_mall.errorcode.ErrorCode.*;

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

    /**
     * @deprecated 请使用 {@link #updateProduct(Long, UpdateProductDTO)} 替代
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public int update(ProductEntity productEntity) {
        if (Objects.isNull(productEntity) || Objects.isNull(productEntity.getId())) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "Product ID cannot be null");
        }

        Long productId = productEntity.getId();
        ProductEntity current = productMapper.selectByPrimaryKey(productId);
        if (Objects.isNull(current)) {
            throw new BusinessException(PRODUCT_NOT_EXIST.getCode(), "Product not found");
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
            throw new BusinessException(RESOURCE_CONFLICT.getCode(), "Product data has been modified, please refresh and retry");
        }
        return rows;
    }

    /**
     * 更新商品（业界标准：字段白名单 + CAS 乐观锁 + 差量更新属性）
     *
     * @param productId 商品ID
     * @param request   更新请求（包含 version 用于并发控制）
     * @return 更新后的商品实体
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductEntity updateProduct(Long productId, UpdateProductDTO request) {
        if (Objects.isNull(productId)) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "Product ID cannot be null");
        }

        // 1. 查询当前商品
        ProductEntity current = productMapper.selectByPrimaryKey(productId);
        if (Objects.isNull(current) || Boolean.TRUE.equals(current.getIsDel())) {
            throw new BusinessException(PRODUCT_NOT_EXIST.getCode(), "Product not found");
        }

        // 2. 版本校验（客户端必须提供与数据库一致的版本号）
        if (!Objects.equals(current.getVersion(), request.getVersion())) {
            log.warn("Version mismatch for productId {}: expected {}, got {}",
                    productId, current.getVersion(), request.getVersion());
            throw new BusinessException(RESOURCE_CONFLICT.getCode(),
                    "Product data has been modified by another user, please refresh and retry");
        }

        // 3. 领域规则校验
        validateDomainRules(current, request);

        // 4. 字段白名单合并（仅允许更新指定字段）
        applyWhitelistFields(current, request);

        // 5. 设置更新信息
        JwtUserEntity currentUser = SecurityUtil.getUserInfo();
        current.setUpdateUserId(currentUser.getId());
        current.setUpdateUserName(currentUser.getUsername());
        current.setUpdateTime(LocalDateTime.now());

        // 6. CAS 更新（SQL 层保证 version 匹配后自增）
        int rows = productMapper.updateProductInfoWithVersion(current);
        if (rows == 0) {
            log.warn("CAS failure for productId {} (version={})", productId, request.getVersion());
            throw new BusinessException(RESOURCE_CONFLICT.getCode(),
                    "Product data has been modified by another user, please refresh and retry");
        }

        // 7. 差量更新 SKU 属性
        if (Objects.nonNull(request.getSkuAttributes())) {
            updateProductAttributesDiff(productId, request.getSkuAttributes(), currentUser);
        }

        // 8. 重新查询并返回更新后的数据
        return productMapper.selectByPrimaryKey(productId);
    }

    /**
     * 领域规则校验
     */
    private void validateDomainRules(ProductEntity current, UpdateProductDTO request) {
        // remainQuantity <= quantity 校验
        Integer newQuantity = Objects.nonNull(request.getQuantity()) ? request.getQuantity() : current.getQuantity();
        Integer newRemainQuantity = Objects.nonNull(request.getRemainQuantity()) ? request.getRemainQuantity() : current.getRemainQuantity();

        if (Objects.nonNull(newQuantity) && Objects.nonNull(newRemainQuantity) && newRemainQuantity > newQuantity) {
            throw new BusinessException(PARAMETER_VALIDATION_ERROR.getCode(),
                    "Remaining quantity cannot be greater than total quantity");
        }
    }

    /**
     * 字段白名单合并：只更新允许更新的字段
     */
    private void applyWhitelistFields(ProductEntity current, UpdateProductDTO request) {
        if (Objects.nonNull(request.getCategoryId())) {
            current.setCategoryId(request.getCategoryId());
        }
        if (Objects.nonNull(request.getBrandId())) {
            current.setBrandId(request.getBrandId());
        }
        if (Objects.nonNull(request.getUnitId())) {
            current.setUnitId(request.getUnitId());
        }
        if (Objects.nonNull(request.getProductGroupId())) {
            current.setProductGroupId(request.getProductGroupId());
        }
        if (Objects.nonNull(request.getName())) {
            current.setName(request.getName());
        }
        if (Objects.nonNull(request.getModel())) {
            current.setModel(request.getModel());
        }
        if (Objects.nonNull(request.getQuantity())) {
            current.setQuantity(request.getQuantity());
        }
        if (Objects.nonNull(request.getRemainQuantity())) {
            current.setRemainQuantity(request.getRemainQuantity());
        }
        if (Objects.nonNull(request.getPrice())) {
            current.setPrice(request.getPrice());
        }
        if (Objects.nonNull(request.getCoverUrl())) {
            current.setCoverUrl(request.getCoverUrl());
        }
    }

    /**
     * SKU 属性差量更新
     * - 有 id 且 deleted=true → 软删除
     * - 有 id 且 deleted!=true → 更新
     * - 无 id → 新增
     */
    private void updateProductAttributesDiff(Long productId,
                                             List<UpdateProductAttributeDTO> attrRequests,
                                             JwtUserEntity currentUser) {
        LocalDateTime now = LocalDateTime.now();

        // 查询当前商品所有未删除的属性
        ProductAttributeEntityExample example = new ProductAttributeEntityExample();
        example.createCriteria()
                .andProductIdEqualTo(productId)
                .andIsDelEqualTo(false);
        List<ProductAttributeEntity> existingAttrs = productAttributeMapper.selectByExample(example);

        // 构建 id -> entity 映射
        Map<Long, ProductAttributeEntity> existingMap = existingAttrs.stream()
                .filter(e -> Objects.nonNull(e.getId()))
                .collect(Collectors.toMap(ProductAttributeEntity::getId, Function.identity()));

        for (UpdateProductAttributeDTO attrReq : attrRequests) {
            if (Objects.nonNull(attrReq.getId())) {
                // 更新或删除
                ProductAttributeEntity existing = existingMap.get(attrReq.getId());
                if (Objects.isNull(existing)) {
                    log.warn("Attribute id {} not found for product {}, skipping", attrReq.getId(), productId);
                    continue;
                }

                if (Boolean.TRUE.equals(attrReq.getDeleted())) {
                    // 软删除
                    existing.setIsDel(true);
                } else {
                    // 更新属性值
                    existing.setAttributeId(attrReq.getAttributeId());
                    existing.setAttributeValueId(attrReq.getAttributeValueId());
                }
                existing.setUpdateUserId(currentUser.getId());
                existing.setUpdateUserName(currentUser.getUsername());
                existing.setUpdateTime(now);
                productAttributeMapper.updateByPrimaryKeySelective(existing);
            } else {
                // 新增
                ProductAttributeEntity newAttr = new ProductAttributeEntity();
                newAttr.setProductId(productId);
                newAttr.setAttributeId(attrReq.getAttributeId());
                newAttr.setAttributeValueId(attrReq.getAttributeValueId());
                newAttr.setCreateUserId(currentUser.getId());
                newAttr.setCreateUserName(currentUser.getUsername());
                newAttr.setCreateTime(now);
                newAttr.setUpdateUserId(currentUser.getId());
                newAttr.setUpdateUserName(currentUser.getUsername());
                newAttr.setUpdateTime(now);
                newAttr.setIsDel(false);
                productAttributeMapper.insertSelective(newAttr);
            }
        }
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
