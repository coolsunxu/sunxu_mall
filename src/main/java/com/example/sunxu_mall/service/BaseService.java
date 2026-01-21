package com.example.sunxu_mall.service;

import com.example.sunxu_mall.dto.BasePageQuery;
import com.example.sunxu_mall.dto.CursorState;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.model.ResponseCursorEntity;
import com.example.sunxu_mall.service.export.ExcelExportService;
import com.example.sunxu_mall.util.CursorTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/10 9:35
 * @description 基础服务类，提供通用的游标分页查询能力（已移除 PageHelper，统一游标分页）
 */
@Slf4j
public abstract class BaseService<K, V extends BasePageQuery> {

    @Autowired
    private ExcelExportService excelExportService;

    /**
     * 子类需实现此方法，根据查询条件返回列表数据（带 limit）
     * SQL 应显式包含 limit 子句
     *
     * @param query 查询条件
     * @param limit 限制条数
     * @return 数据列表
     */
    protected abstract List<K> selectListWithLimit(V query, int limit);

    /**
     * 子类需实现此方法，根据游标条件返回列表数据（带 limit）
     * SQL 应显式包含：where id < cursorId order by id desc limit #{limit}
     *
     * @param query    查询条件
     * @param cursorId 游标 ID（可为 null，表示从头开始）
     * @param limit    限制条数
     * @return 数据列表
     */
    protected abstract List<K> selectListByCursorWithLimit(V query, Long cursorId, int limit);

    /**
     * 子类需实现此方法，提取实体的 ID 用作游标
     *
     * @param entity 实体对象
     * @return 实体 ID
     */
    protected abstract Long extractEntityId(K entity);

    /**
     * 双向游标分页查询，支持在已访问页内跳转
     * <p>
     * 游标语义：pageCursorMap 存储 pageNum -> afterCursor（该页最后一条记录的 ID）
     * - 第 1 页：cursorId = null（从头开始）
     * - 第 N 页（N > 1）：cursorId = state.getCursorForPage(N - 1)，即上一页的 afterCursor
     * <p>
     * 注意：不支持直接跳到未访问的页，必须从第 1 页逐页翻页或在已访问页内跳转
     *
     * @param query 查询条件
     * @return 游标分页结果
     */
    public ResponseCursorEntity<K> searchByBidirectionalCursor(V query) {
        Integer pageSize = query.getPageSize();
        Integer pageNum = query.getPageNum();
        String cursorToken = query.getCursorToken();

        // 参数校验和默认值
        if (Objects.isNull(pageSize) || pageSize < 1) {
            pageSize = 10;
        }
        if (Objects.isNull(pageNum) || pageNum < 1) {
            pageNum = 1;
        }

        // 解码游标状态
        CursorState state = CursorTokenUtil.decode(cursorToken);
        if (Objects.isNull(state)) {
            // 首次查询，创建初始状态
            state = CursorTokenUtil.createInitialState(pageNum, pageSize);
        } else {
            // P1：语义自洽 - token 绑定 pageSize，避免用户篡改导致“已访问页”的游标语义错乱
            if (Objects.nonNull(state.getPageSize()) && !state.getPageSize().equals(pageSize)) {
                throw new BusinessException("pageSize与cursorToken不一致，请重新从第一页查询");
            }
        }

        // 计算查询用的游标 ID
        // - 第 1 页：cursorId = null
        // - 第 N 页（N > 1）：cursorId = state.getCursorForPage(N - 1)
        Long cursorId = null;
        if (pageNum > 1) {
            cursorId = state.getCursorForPage(pageNum - 1);
            // 未访问页拦截：如果请求的页码 > 1 但没有前一页的游标，说明用户跳到了未访问的页
            if (Objects.isNull(cursorId)) {
                throw new BusinessException("只能跳转到已访问页，请从第一页逐页翻页后再尝试跳转");
            }
        }

        // 执行游标查询（始终使用 NEXT 方向：id < cursorId order by id desc limit #{fetchSize}）
        int fetchSize = pageSize + 1;
        List<K> dataList = selectListByCursorWithLimit(query, cursorId, fetchSize);

        // 判断是否有下一页
        boolean hasNext = Objects.nonNull(dataList) && dataList.size() > pageSize;
        if (hasNext) {
            dataList = dataList.subList(0, pageSize);
        }

        // 获取当前页最后一条记录的 ID 作为本页的 afterCursor
        Long afterCursor = null;
        if (Objects.nonNull(dataList) && !dataList.isEmpty()) {
            afterCursor = extractEntityId(dataList.get(dataList.size() - 1));
        }

        // 更新游标状态
        state.setPageNum(pageNum);
        state.setPageSize(pageSize);
        state.setLastId(afterCursor);

        // 记录当前页的 afterCursor，供后续页使用
        if (Objects.nonNull(afterCursor)) {
            state.setCursorForPage(pageNum, afterCursor);
        }

        // 生成新的游标令牌
        String newCursorToken = CursorTokenUtil.encode(state);

        // 判断是否有上一页
        boolean hasPrev = pageNum > 1;
        // P1：补齐响应语义 - prevCursorId 表示“查询上一页所需的 cursorId（即上一页的上一页 afterCursor）”
        // - 上一页是第 1 页时，cursorId = null
        // - 上一页是第 M 页(M>1)时，cursorId = state.getCursorForPage(M-1)
        Long prevCursorId = null;
        if (pageNum > 2) {
            prevCursorId = state.getCursorForPage(pageNum - 2);
        }

        // 构建响应
        ResponseCursorEntity<K> response = new ResponseCursorEntity<>();
        response.setPageSize((long) pageSize);
        response.setNextCursorId(afterCursor);
        response.setHasNext(hasNext);
        response.setList(dataList);
        response.setCursorToken(newCursorToken);
        response.setCurrentPageNum(pageNum);
        response.setPrevCursorId(prevCursorId);
        response.setHasPrev(hasPrev);

        return response;
    }

    /**
     * 公共excel导出方法（使用游标分页，不再依赖 PageHelper）
     *
     * @param query     查询条件
     * @param fileName  文件名称
     * @param clazzName 实体类名称
     * @return 下载地址
     */
    public String export(V query, String fileName, String clazzName) {
        return excelExportService.export(
                query,
                fileName,
                clazzName,
                this::selectListByCursorWithLimit,
                this::extractEntityId
        );
    }
}
