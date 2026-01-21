package com.example.sunxu_mall.service.export;

import com.example.sunxu_mall.dto.BasePageQuery;

import java.util.List;

/**
 * 游标分页拉取器（用于导出等需要“按游标循环拉取”的场景）
 */
@FunctionalInterface
public interface CursorPageFetcher<K, V extends BasePageQuery> {

    /**
     * @param query    查询条件
     * @param cursorId 游标（可为 null，表示从头开始）
     * @param limit    拉取条数
     */
    List<K> fetch(V query, Long cursorId, int limit);
}

