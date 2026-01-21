package com.example.sunxu_mall.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

@Data
@Schema(description = "Cursor Page Response")
public class ResponseCursorEntity<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Page size")
    private Long pageSize;

    @Schema(description = "Next cursor id")
    private Long nextCursorId;

    @Schema(description = "Whether has next page")
    private Boolean hasNext;

    @Schema(description = "Data list")
    private List<T> list;

    @Schema(description = "Cursor token for bidirectional pagination")
    private String cursorToken;

    @Schema(description = "Current page number")
    private Integer currentPageNum;

    @Schema(description = "Previous cursor id for backward navigation")
    private Long prevCursorId;

    @Schema(description = "Whether has previous page")
    private Boolean hasPrev;

    public ResponseCursorEntity() {
    }

    public ResponseCursorEntity(Long pageSize, Long nextCursorId, Boolean hasNext, List<T> list) {
        this.pageSize = pageSize;
        this.nextCursorId = nextCursorId;
        this.hasNext = hasNext;
        this.list = list;
    }

    /**
     * 将ResponseCursorEntity<T>转换为ResponseCursorEntity<R>
     *
     * @param source        源ResponseCursorEntity
     * @param listConverter 列表转换函数
     * @param <T>        源数据类型
     * @param <R>        目标数据类型
     * @return 转换后的ResponseCursorEntity
     */
    public static <T, R> ResponseCursorEntity<R> convert(ResponseCursorEntity<T> source, Function<List<T>, List<R>> listConverter) {
        ResponseCursorEntity<R> target = new ResponseCursorEntity<>();
        target.setPageSize(source.getPageSize());
        target.setNextCursorId(source.getNextCursorId());
        target.setHasNext(source.getHasNext());
        target.setList(listConverter.apply(source.getList()));
        target.setCursorToken(source.getCursorToken());
        target.setCurrentPageNum(source.getCurrentPageNum());
        target.setPrevCursorId(source.getPrevCursorId());
        target.setHasPrev(source.getHasPrev());
        return target;
    }
}

