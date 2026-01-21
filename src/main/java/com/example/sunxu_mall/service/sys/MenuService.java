package com.example.sunxu_mall.service.sys;

import org.springframework.util.CollectionUtils;
import com.example.sunxu_mall.dto.sys.MenuTreeDTO;
import com.example.sunxu_mall.entity.sys.MenuEntity;
import com.example.sunxu_mall.entity.sys.MenuEntityExample;
import com.example.sunxu_mall.mapper.sys.MenuEntityMapper;
import com.example.sunxu_mall.util.BeanCopyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/27 18:04
 * @description
 */

@Slf4j
@Service
@org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
public class MenuService {

    private final MenuEntityMapper menuEntityMapper;

    public MenuService(
            MenuEntityMapper menuEntityMapper
    ) {
        this.menuEntityMapper = menuEntityMapper;
    }

    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void updateMenu(MenuEntity menu) {
        MenuEntity dbMenu = menuEntityMapper.selectByPrimaryKey(menu.getMenuId());
        if (Objects.isNull(dbMenu)) {
            throw new RuntimeException("Menu not found");
        }
        BeanCopyUtils.copyNonNullProperties(menu, dbMenu);

        int rows = menuEntityMapper.updateBasicInfoWithVersion(dbMenu);
        if (rows == 0) {
            throw new OptimisticLockingFailureException("Menu has been modified by others");
        }
    }

    /**
     * 获取菜单树
     *
     * @return 菜单树
     */
    public List<MenuTreeDTO> getMenuTree() {
        MenuEntityExample example = new MenuEntityExample();
        example.createCriteria().andParentIdEqualTo(0);
        example.setOrderByClause("sort_number desc");
        List<MenuEntity> menuEntities = menuEntityMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(menuEntities)) {
            return Collections.emptyList();
        }

        List<MenuTreeDTO> result = new ArrayList<>();
        for (MenuEntity menuEntity : menuEntities) {
            MenuTreeDTO menuTreeDTO = buildMenuTreeDTO(menuEntity);
            menuTreeDTO.setAlwaysShow(true);
            result.add(menuTreeDTO);
            buildChildren(menuEntity, menuTreeDTO);
        }

        log.info("getMenuTree: {}", result);
        return result;
    }

    private MenuTreeDTO buildMenuTreeDTO(MenuEntity menuEntity) {

        return MenuTreeDTO.builder()
                .id(menuEntity.getMenuId().longValue())
                .label(menuEntity.getTitle())
                .pid(menuEntity.getParentId().longValue())
                .sort(menuEntity.getSortNumber())
                .icon(menuEntity.getIcon())
                .path(menuEntity.getPath())
                .hidden(Objects.nonNull(menuEntity.getHide()) && menuEntity.getHide() == 1)
                .component(menuEntity.getComponent())
                .type(menuEntity.getMenuType())
                .permission(menuEntity.getAuthority())
                .build();
    }

    private void buildChildren(MenuEntity parentEntity, MenuTreeDTO parentDTO) {
        MenuEntityExample example = new MenuEntityExample();
        example.createCriteria().andParentIdEqualTo(parentEntity.getMenuId());
        example.setOrderByClause("sort_number desc");
        List<MenuEntity> children = menuEntityMapper.selectByExample(example);
        
        if (!CollectionUtils.isEmpty(children)) {
            List<MenuTreeDTO> childrenDTO = new ArrayList<>();
            for (MenuEntity child : children) {
                MenuTreeDTO childDTO = buildMenuTreeDTO(child);
                childrenDTO.add(childDTO);
                buildChildren(child, childDTO);
            }
            parentDTO.setChildren(childrenDTO);
        }
    }
}
