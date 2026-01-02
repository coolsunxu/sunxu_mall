package com.example.sunxu_mall.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "Page Response")
public class ResponsePageEntity<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Current page number")
    private Long pageNum;

    @Schema(description = "Page size")
    private Long pageSize;

    @Schema(description = "Total records")
    private Long total;

    @Schema(description = "Data list")
    private List<T> list;
    
    public ResponsePageEntity() {}
    
    public ResponsePageEntity(Long pageNum, Long pageSize, Long total, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
    }
}
