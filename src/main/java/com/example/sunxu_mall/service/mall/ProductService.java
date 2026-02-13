package com.example.sunxu_mall.service.mall;

import cn.hutool.crypto.digest.DigestUtil;
import com.example.sunxu_mall.dto.mall.*;
import com.example.sunxu_mall.entity.mall.*;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.mall.*;
import com.example.sunxu_mall.service.BaseService;
import com.example.sunxu_mall.util.BeanCopyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.sunxu_mall.errorcode.ErrorCode.*;

/**
 * 商品服务类
 * 负责商品的增删改查、商品组管理、属性管理、图片管理等核心业务逻辑
 *
 * @author sunxu
 * @since 1.0.0
 */
@Slf4j
@Service
public class ProductService extends BaseService<ProductEntity, ProductQueryDTO> {

    /**
     * 商品初始版本号
     */
    private static final int INITIAL_VERSION = 1;

    /**
     * 图片排序初始值
     */
    private static final int INITIAL_SORT = 1;

    /**
     * 轮播图类型标识
     */
    private static final boolean SWIPER_PHOTO_TYPE = false;

    private final ProductEntityMapper productMapper;
    private final ProductGroupAttributeEntityMapper productGroupAttributeMapper;
    private final ProductAttributeEntityMapper productAttributeMapper;
    private final AttributeValueEntityMapper attributeValueMapper;
    private final CommonPhotoEntityMapper commonPhotoMapper;
    private final MallProductGroupEntityMapper productGroupMapper;
    private final MallProductDetailEntityMapper productDetailMapper;
    private final MallProductPhotoEntityMapper productPhotoMapper;

    public ProductService(
            ProductEntityMapper productMapper,
            ProductGroupAttributeEntityMapper productGroupAttributeMapper,
            ProductAttributeEntityMapper productAttributeMapper,
            AttributeValueEntityMapper attributeValueMapper,
            CommonPhotoEntityMapper commonPhotoMapper,
            MallProductGroupEntityMapper productGroupMapper,
            MallProductDetailEntityMapper productDetailMapper,
            MallProductPhotoEntityMapper productPhotoMapper
    ) {
        this.productMapper = productMapper;
        this.productGroupAttributeMapper = productGroupAttributeMapper;
        this.productAttributeMapper = productAttributeMapper;
        this.attributeValueMapper = attributeValueMapper;
        this.commonPhotoMapper = commonPhotoMapper;
        this.productGroupMapper = productGroupMapper;
        this.productDetailMapper = productDetailMapper;
        this.productPhotoMapper = productPhotoMapper;
    }

    /**
     * 根据商品ID查询商品详情
     * 包含商品基本信息、SPU属性、SKU属性、图片和详情描述
     *
     * @param id 商品ID
     * @return 商品详情DTO，如果商品不存在则返回null
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
        fillDetail(productDetailDTO);

        return productDetailDTO;
    }

    /**
     * 填充商品详情描述信息
     *
     * @param productDetailDTO 商品详情DTO
     */
    private void fillDetail(ProductDetailDTO productDetailDTO) {
        MallProductDetailEntityExample example = new MallProductDetailEntityExample();
        example.createCriteria().andProductIdEqualTo(productDetailDTO.getId()).andIsDelEqualTo(false);
        List<MallProductDetailEntity> details = productDetailMapper.selectByExampleWithBLOBs(example);
        if (CollectionUtils.isNotEmpty(details)) {
            byte[] content = details.get(0).getDetail();
            if (content != null) {
                productDetailDTO.setDetail(new String(content, StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * 创建新商品
     * 包含商品组处理、商品信息保存、SKU属性关联、图片上传等完整流程
     * 使用哈希算法确保商品唯一性，避免重复创建
     *
     * @param dto 创建商品请求DTO
     * @return 创建成功的商品实体
     * @throws BusinessException 当商品规格已存在时抛出资源冲突异常
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductEntity createProduct(CreateProductDTO dto) {
        // 1. 查找或创建商品组
        Long productGroupId = findOrCreateProductGroup(dto);

        // 2. 创建商品实体
        ProductEntity product = createProductEntity(dto, productGroupId);
        Long productId = product.getId();

        // 3. 保存关联数据
        saveProductDetail(productId, dto.getDetail());
        saveSkuAttributes(productId, dto.getSkuAttributes());
        saveProductPhotos(productId, dto.getPhotos());

        return product;
    }

    /**
     * 查找或创建商品组
     * 根据分类、单位和SPU属性查找已存在的商品组，如不存在则创建新商品组
     *
     * @param dto 创建商品请求DTO
     * @return 商品组ID
     */
    private Long findOrCreateProductGroup(CreateProductDTO dto) {
        String groupHash = generateGroupHash(dto.getCategoryId(), dto.getUnitId(), dto.getSpuAttributes());

        // 查询是否已存在相同的商品组
        MallProductGroupEntityExample groupExample = new MallProductGroupEntityExample();
        groupExample.createCriteria()
                .andCategoryIdEqualTo(dto.getCategoryId())
                .andUnitIdEqualTo(dto.getUnitId())
                .andHashEqualTo(groupHash)
                .andIsDelEqualTo(false);
        List<MallProductGroupEntity> groups = productGroupMapper.selectByExample(groupExample);

        if (CollectionUtils.isNotEmpty(groups)) {
            return groups.get(0).getId();
        }

        // 创建新商品组
        MallProductGroupEntity group = MallProductGroupEntity.builder()
                .categoryId(dto.getCategoryId())
                .unitId(dto.getUnitId())
                .name(dto.getName())
                .model(dto.getModel())
                .hash(groupHash)
                .isDel(false)
                .build();
        productGroupMapper.insertSelective(group);
        Long productGroupId = group.getId();

        // 保存 SPU 属性
        saveSpuAttributes(productGroupId, dto.getSpuAttributes());

        return productGroupId;
    }

    /**
     * 保存SPU属性
     *
     * @param productGroupId 商品组ID
     * @param spuAttributes  SPU属性列表
     */
    private void saveSpuAttributes(Long productGroupId, List<CreateProductAttributeDTO> spuAttributes) {
        if (CollectionUtils.isEmpty(spuAttributes)) {
            return;
        }

        for (CreateProductAttributeDTO attr : spuAttributes) {
            ProductGroupAttributeEntity pga = ProductGroupAttributeEntity.builder()
                    .productGroupId(productGroupId)
                    .attributeId(attr.getAttributeId())
                    .attributeValueId(attr.getAttributeValueId())
                    .isDel(false)
                    .build();
            productGroupAttributeMapper.insertSelective(pga);
        }
    }

    /**
     * 创建商品实体
     * 验证商品是否已存在，然后创建新商品记录
     *
     * @param dto            创建商品请求DTO
     * @param productGroupId 商品组ID
     * @return 创建成功的商品实体
     * @throws BusinessException 当商品规格已存在时抛出资源冲突异常
     */
    private ProductEntity createProductEntity(CreateProductDTO dto, Long productGroupId) {
        String productHash = generateProductHash(productGroupId, dto.getBrandId(), dto.getSkuAttributes());

        // 验证商品是否已存在
        checkProductNotExists(productGroupId, dto.getBrandId(), productHash);

        // 创建商品实体
        ProductEntity product = ProductEntity.builder()
                .categoryId(dto.getCategoryId())
                .productGroupId(productGroupId)
                .brandId(dto.getBrandId())
                .unitId(dto.getUnitId())
                .name(dto.getName())
                .model(dto.getModel())
                .quantity(dto.getQuantity())
                .remainQuantity(dto.getQuantity())
                .price(dto.getPrice())
                .coverUrl(dto.getCoverUrl())
                .hash(productHash)
                .isDel(false)
                .build();
        productMapper.insertSelective(product);

        return product;
    }

    /**
     * 检查商品是否已存在
     *
     * @param productGroupId 商品组ID
     * @param brandId        品牌ID
     * @param productHash    商品哈希值
     * @throws BusinessException 当商品已存在时抛出异常
     */
    private void checkProductNotExists(Long productGroupId, Long brandId, String productHash) {
        ProductEntityExample productExample = new ProductEntityExample();
        productExample.createCriteria()
                .andProductGroupIdEqualTo(productGroupId)
                .andBrandIdEqualTo(brandId)
                .andHashEqualTo(productHash)
                .andIsDelEqualTo(false);

        if (productMapper.countByExample(productExample) > 0) {
            throw new BusinessException(RESOURCE_CONFLICT.getCode(), "该规格商品已存在");
        }
    }

    /**
     * 保存商品详情
     *
     * @param productId 商品ID
     * @param detail    商品详情内容
     */
    private void saveProductDetail(Long productId, String detail) {
        if (StringUtils.isBlank(detail)) {
            return;
        }

        MallProductDetailEntity detailEntity = MallProductDetailEntity.builder()
                .productId(productId)
                .detail(detail.getBytes(StandardCharsets.UTF_8))
                .isDel(false)
                .build();
        productDetailMapper.insertSelective(detailEntity);
    }

    /**
     * 保存SKU属性
     *
     * @param productId     商品ID
     * @param skuAttributes SKU属性列表
     */
    private void saveSkuAttributes(Long productId, List<CreateProductAttributeDTO> skuAttributes) {
        if (CollectionUtils.isEmpty(skuAttributes)) {
            return;
        }

        for (CreateProductAttributeDTO attr : skuAttributes) {
            ProductAttributeEntity pa = ProductAttributeEntity.builder()
                    .productId(productId)
                    .attributeId(attr.getAttributeId())
                    .attributeValueId(attr.getAttributeValueId())
                    .isDel(false)
                    .build();
            productAttributeMapper.insertSelective(pa);
        }
    }

    /**
     * 保存商品图片
     *
     * @param productId 商品ID
     * @param photos    图片URL列表
     */
    private void saveProductPhotos(Long productId, List<String> photos) {
        if (CollectionUtils.isEmpty(photos)) {
            return;
        }

        int sort = INITIAL_SORT;
        for (String url : photos) {
            String name = url.substring(url.lastIndexOf('/') + 1);
            MallProductPhotoEntity photo = MallProductPhotoEntity.builder()
                    .productId(productId)
                    .name(name)
                    .url(url)
                    .sort(sort++)
                    .type(SWIPER_PHOTO_TYPE)
                    .isDel(false)
                    .build();
            productPhotoMapper.insertSelective(photo);
        }
    }

    /**
     * 生成商品组哈希值
     * 基于分类ID、单位ID和SPU属性列表生成唯一哈希
     *
     * @param categoryId 分类ID
     * @param unitId     单位ID
     * @param attrs      SPU属性列表
     * @return MD5哈希字符串
     */
    private String generateGroupHash(Long categoryId, Long unitId, List<CreateProductAttributeDTO> attrs) {
        return generateAttrHash(categoryId + ":" + unitId, attrs);
    }

    /**
     * 生成商品哈希值
     * 基于商品组ID、品牌ID和SKU属性列表生成唯一哈希
     *
     * @param groupId 商品组ID
     * @param brandId 品牌ID
     * @param attrs   SKU属性列表
     * @return MD5哈希字符串
     */
    private String generateProductHash(Long groupId, Long brandId, List<CreateProductAttributeDTO> attrs) {
        return generateAttrHash(groupId + ":" + brandId, attrs);
    }

    /**
     * 生成属性哈希值
     * 对属性列表按ID排序后生成MD5哈希，确保相同属性组合生成相同哈希
     *
     * @param prefix 哈希前缀（如分类:单位 或 组:品牌）
     * @param attrs  属性列表
     * @return MD5哈希字符串
     */
    private String generateAttrHash(String prefix, List<CreateProductAttributeDTO> attrs) {
        if (CollectionUtils.isEmpty(attrs)) {
            return DigestUtil.md5Hex(prefix);
        }
        String attrStr = attrs.stream()
                .sorted(Comparator.comparing(CreateProductAttributeDTO::getAttributeId)
                        .thenComparing(CreateProductAttributeDTO::getAttributeValueId))
                .map(a -> a.getAttributeId() + ":" + a.getAttributeValueId())
                .collect(Collectors.joining(","));
        return DigestUtil.md5Hex(prefix + "|" + attrStr);
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
     * 插入新商品（简化版本）
     * 仅保存商品基本信息，不处理关联数据
     *
     * @param productEntity 商品实体
     * @throws BusinessException 当商品实体为null时抛出参数缺失异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(ProductEntity productEntity) {
        if (Objects.isNull(productEntity)) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "商品实体不能为空");
        }

        productEntity.setIsDel(false);
        productEntity.setVersion(INITIAL_VERSION);

        productMapper.insertSelective(productEntity);
    }

    /**
     * @deprecated 请使用 {@link #updateProduct(Long, UpdateProductDTO)} 替代
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public int update(ProductEntity productEntity) {
        if (Objects.isNull(productEntity) || Objects.isNull(productEntity.getId())) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "商品ID不能为空");
        }

        Long productId = productEntity.getId();
        ProductEntity current = productMapper.selectByPrimaryKey(productId);
        if (Objects.isNull(current)) {
            throw new BusinessException(PRODUCT_NOT_EXIST.getCode(), "商品不存在");
        }

        Integer oldVersion = current.getVersion();
        BeanCopyUtils.copyNonNullProperties(productEntity, current);

        current.setVersion(oldVersion);

        int rows = productMapper.updateProductInfoWithVersion(current);
        if (rows == 0) {
            log.warn("乐观锁更新失败，productId={}", productId);
            throw new BusinessException(RESOURCE_CONFLICT.getCode(), "商品数据已被修改，请刷新后重试");
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
            throw new BusinessException(PARAMETER_MISSING.getCode(), "商品ID不能为空");
        }

        // 1. 查询当前商品
        ProductEntity current = productMapper.selectByPrimaryKey(productId);
        if (Objects.isNull(current) || Boolean.TRUE.equals(current.getIsDel())) {
            throw new BusinessException(PRODUCT_NOT_EXIST.getCode(), "商品不存在");
        }

        // 2. 版本校验（客户端必须提供与数据库一致的版本号）
        if (!Objects.equals(current.getVersion(), request.getVersion())) {
            log.warn("商品版本不匹配，productId={}, 期望版本={}, 实际版本={}",
                    productId, current.getVersion(), request.getVersion());
            throw new BusinessException(RESOURCE_CONFLICT.getCode(),
                    "商品数据已被其他用户修改，请刷新后重试");
        }

        // 3. 领域规则校验
        validateDomainRules(current, request);

        // 4. 字段白名单合并（仅允许更新指定字段）
        applyWhitelistFields(current, request);

        // 5. 设置更新信息

        // 6. CAS 更新（SQL 层保证 version 匹配后自增）
        int rows = productMapper.updateProductInfoWithVersion(current);
        if (rows == 0) {
            log.warn("CAS更新失败，productId={}, version={}", productId, request.getVersion());
            throw new BusinessException(RESOURCE_CONFLICT.getCode(),
                    "商品数据已被其他用户修改，请刷新后重试");
        }

        // 7. 差量更新 SKU 属性
        if (Objects.nonNull(request.getSkuAttributes())) {
            updateProductAttributesDiff(productId, request.getSkuAttributes());
        }

        // 8. 重新查询并返回更新后的数据
        return productMapper.selectByPrimaryKey(productId);
    }

    /**
     * 领域规则校验
     * 校验业务规则，如剩余数量不能大于总数量
     *
     * @param current 当前商品实体
     * @param request 更新请求DTO
     * @throws BusinessException 当业务规则校验失败时抛出参数校验异常
     */
    private void validateDomainRules(ProductEntity current, UpdateProductDTO request) {
        // 校验：剩余数量不能大于总数量
        Integer newQuantity = Objects.nonNull(request.getQuantity()) ? request.getQuantity()
                : current.getQuantity();
        Integer newRemainQuantity = Objects.nonNull(request.getRemainQuantity()) ? request.getRemainQuantity()
                : current.getRemainQuantity();

        if (Objects.nonNull(newQuantity) && Objects.nonNull(newRemainQuantity)
                && newRemainQuantity > newQuantity) {
            throw new BusinessException(PARAMETER_VALIDATION_ERROR.getCode(),
                    "剩余数量不能大于总数量");
        }
    }

    /**
     * 字段白名单合并
     * 只更新允许更新的字段，防止恶意更新敏感字段
     *
     * @param current 当前商品实体
     * @param request 更新请求DTO
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
     * 支持三种操作：新增、更新、软删除
     * - 有 id 且 deleted=true：软删除该属性
     * - 有 id 且 deleted!=true：更新属性值
     * - 无 id：新增属性
     *
     * @param productId    商品ID
     * @param attrRequests 属性更新请求列表
     */
    private void updateProductAttributesDiff(Long productId,
                                             List<UpdateProductAttributeDTO> attrRequests) {
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
                    log.warn("商品属性不存在，attributeId={}, productId={}, 跳过处理",
                            attrReq.getId(), productId);
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
                productAttributeMapper.updateByPrimaryKeySelective(existing);
            } else {
                // 新增
                ProductAttributeEntity newAttr = ProductAttributeEntity.builder()
                        .productId(productId)
                        .attributeId(attrReq.getAttributeId())
                        .attributeValueId(attrReq.getAttributeValueId())
                        .isDel(false)
                        .build();
                productAttributeMapper.insertSelective(newAttr);
            }
        }
    }

    /**
     * 批量删除商品（软删除）
     * 仅删除商品本身，不处理关联数据
     *
     * @param ids 商品ID列表
     * @return 删除的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        ProductEntityExample example = new ProductEntityExample();
        example.createCriteria().andIdIn(ids);

        ProductEntity update = ProductEntity.builder()
                .isDel(true)
                .build();

        return productMapper.updateByExampleSelective(update, example);
    }

    /**
     * 删除商品（软删除）并级联软删相关数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        if (Objects.isNull(productId)) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "商品ID不能为空");
        }

        ProductEntity current = productMapper.selectByPrimaryKey(productId);
        if (Objects.isNull(current) || Boolean.TRUE.equals(current.getIsDel())) {
            throw new BusinessException(PRODUCT_NOT_EXIST.getCode(), "商品不存在");
        }

        // 1) 软删商品本身
        ProductEntity updateProduct = ProductEntity.builder()
                .id(productId)
                .isDel(true)
                .delId(productId)
                .build();
        productMapper.updateByPrimaryKeySelective(updateProduct);

        // 2) 级联软删 SKU 属性（mall_product_attribute）
        ProductAttributeEntityExample paExample = new ProductAttributeEntityExample();
        paExample.createCriteria().andProductIdEqualTo(productId).andIsDelEqualTo(false);
        ProductAttributeEntity paUpdate = ProductAttributeEntity.builder()
                .isDel(true)
                .build();
        productAttributeMapper.updateByExampleSelective(paUpdate, paExample);

        // 3) 级联软删详情（mall_product_detail）
        MallProductDetailEntityExample detailExample = new MallProductDetailEntityExample();
        detailExample.createCriteria().andProductIdEqualTo(productId).andIsDelEqualTo(false);
        MallProductDetailEntity detailUpdate = MallProductDetailEntity.builder()
                .isDel(true)
                .build();
        productDetailMapper.updateByExampleSelective(detailUpdate, detailExample);

        // 4) 级联软删图片（mall_product_photo）
        MallProductPhotoEntityExample photoExample = new MallProductPhotoEntityExample();
        photoExample.createCriteria().andProductIdEqualTo(productId).andIsDelEqualTo(false);
        MallProductPhotoEntity photoUpdate = MallProductPhotoEntity.builder()
                .isDel(true)
                .build();
        productPhotoMapper.updateByExampleSelective(photoUpdate, photoExample);

        // 5) 兼容旧图片表（common_photo），按 photo_group_id = productId 软删
        CommonPhotoEntityExample commonPhotoExample = new CommonPhotoEntityExample();
        commonPhotoExample.createCriteria().andPhotoGroupIdEqualTo(productId).andIsDelEqualTo(false);
        CommonPhotoEntity commonPhotoUpdate = CommonPhotoEntity.builder()
                .isDel(true)
                .build();
        commonPhotoMapper.updateByExampleSelective(commonPhotoUpdate, commonPhotoExample);
    }

    /**
     * 填充SPU属性值信息
     * 根据商品组ID查询并填充SPU级别的属性
     *
     * @param productDetailDTO 商品详情DTO
     */
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

    /**
     * 填充SKU属性值信息
     * 根据商品ID查询并填充SKU级别的属性
     *
     * @param productDetailDTO 商品详情DTO
     */
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

    /**
     * 根据属性值ID列表查询属性值实体
     *
     * @param attributeValueIds 属性值ID列表
     * @return 属性值实体列表，如果ID列表为空则返回空列表
     */
    private List<AttributeValueEntity> getAttributeValues(List<Long> attributeValueIds) {
        if (CollectionUtils.isEmpty(attributeValueIds)) {
            return Collections.emptyList();
        }

        AttributeValueEntityExample example = new AttributeValueEntityExample();
        example.createCriteria().andIdIn(attributeValueIds);

        return attributeValueMapper.selectByExample(example);
    }

    /**
     * 填充商品图片信息
     * 包括轮播图和封面图
     *
     * @param productDetailDTO 商品详情DTO
     */
    private void fillPhoto(ProductDetailDTO productDetailDTO) {
        // 查询商品图片（photoGroupId 对应商品ID）
        CommonPhotoEntityExample example = new CommonPhotoEntityExample();
        example.createCriteria().andPhotoGroupIdEqualTo(productDetailDTO.getId());
        List<CommonPhotoEntity> photos = commonPhotoMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(photos)) {
            return;
        }

        // 提取图片URL列表
        List<String> photoUrls = photos.stream()
                .map(CommonPhotoEntity::getUrl)
                .collect(Collectors.toList());

        productDetailDTO.setSwiper(photoUrls);

        // 设置封面图：优先使用已设置的coverUrl，否则使用第一张图片
        if (StringUtils.isBlank(productDetailDTO.getCoverUrl()) && !photoUrls.isEmpty()) {
            productDetailDTO.setCover(Collections.singletonList(photoUrls.get(0)));
        } else if (StringUtils.isNotBlank(productDetailDTO.getCoverUrl())) {
            productDetailDTO.setCover(Collections.singletonList(productDetailDTO.getCoverUrl()));
        }
    }
}
