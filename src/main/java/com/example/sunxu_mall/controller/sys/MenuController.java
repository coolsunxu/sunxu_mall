package com.example.sunxu_mall.controller.sys;

import com.example.sunxu_mall.dto.sys.MenuTreeDTO;
import com.example.sunxu_mall.service.sys.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/27 18:00
 * @description
 */

@Tag(name = "菜单操作", description = "菜单接口")
@RestController
@RequestMapping("/v1/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 获取菜单树
     *
     * @return 菜单列表
     */
    @Operation(summary = "获取菜单树", description = "获取菜单树")
    @GetMapping("/getMenuTree")
    public List<MenuTreeDTO> getMenuTree() {
        return menuService.getMenuTree();
    }

}
