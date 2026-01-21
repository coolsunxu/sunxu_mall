package com.example.sunxu_mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author sunxu
 */
@Data
@Schema(description = "Cursor State for Bidirectional Pagination")
public class CursorState implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Current page number")
    private Integer pageNum;

    @Schema(description = "Page size")
    private Integer pageSize;

    @Schema(description = "Last entity ID")
    private Long lastId;

    @Schema(description = "Page number to cursor ID mapping")
    private Map<Integer, Long> pageCursorMap = new HashMap<>();

    public CursorState() {
    }

    public CursorState(Integer pageNum, Integer pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /**
     * Get cursor ID for specific page
     * @param pageNum page number
     * @return cursor ID or null if not found
     */
    public Long getCursorForPage(Integer pageNum) {
        return Objects.nonNull(pageCursorMap) ? pageCursorMap.get(pageNum) : null;
    }

    /**
     * Set cursor ID for specific page
     * @param pageNum page number
     * @param cursorId cursor ID
     */
    public void setCursorForPage(Integer pageNum, Long cursorId) {
        if (Objects.isNull(pageCursorMap)) {
            pageCursorMap = new HashMap<>(10);
        }
        pageCursorMap.put(pageNum, cursorId);
    }
}