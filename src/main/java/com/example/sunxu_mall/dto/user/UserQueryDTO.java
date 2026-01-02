package com.example.sunxu_mall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

@Data
@Schema(description = "User Query DTO")
public class UserQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Page number", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "Page size", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "Username")
    private String userName;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Valid status")
    private Boolean validStatus;

    @Schema(description = "Department ID")
    private Long deptId;
}
