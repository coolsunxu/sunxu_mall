package com.example.sunxu_mall.dto.sys;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/27 18:06
 * @description
 */

@Data
@Builder
public class MenuTreeDTO implements Serializable {

    /**
     * 菜单系统ID
     */
    @Schema(description = "菜单系统ID")
    private Long id;

    /**
     * 菜单名称
     */
    @Schema(description = "菜单名称")
    private String label;

    /**
     * 上级菜单ID
     */
    @Schema(description = "上级菜单ID")
    private Long pid;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 图标
     */
    @Schema(description = "图标")
    private String icon;

    /**
     * 路由
     */
    @Schema(description = "路由")
    private String path;

    /**
     * 是否隐藏
     */
    @Schema(description = "是否隐藏")
    private Boolean hidden;

    /**
     * 是否外链 1：是 0：否
     */
    @Schema(description = "是否外链 1：是 0：否")
    private Integer isLink;

    /**
     * 类型 1：目录 2：菜单 3：按钮
     */
    @Schema(description = "类型 1：目录 2：菜单 3：按钮")
    private Integer type;

    /**
     * 功能权限
     */
    @Schema(description = "功能权限")
    private String permission;

    /**
     * url地址
     */
    @Schema(description = "url地址")
    private String url;

    /**
     * 组件
     */
    @Schema(description = "组件")
    private String component;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 跳转
     */
    private String redirect;

    private Boolean alwaysShow;


    private MetaDTO meta;

    /**
     * 下一级菜单集合
     */
    private List<MenuTreeDTO> children;

    /**
     * 是否叶子节点
     */
    private Boolean leaf;

    /**
     * 下级菜单数量
     */
    private Integer subCount;

    private Boolean hasChildren;


    /**
     * 增加添加子菜单的方法
     *
     * @param menuTreeDTO 子菜单
     */
    public void addChildren(MenuTreeDTO menuTreeDTO) {
        if (Objects.isNull(children)) {
            children = new ArrayList<>();
        }
        children.add(menuTreeDTO);
    }
}

