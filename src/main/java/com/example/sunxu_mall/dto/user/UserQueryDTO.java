package com.example.sunxu_mall.dto.user;

import com.example.sunxu_mall.dto.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

@Data
@Schema(description = "User Query DTO")
public class UserQueryDTO extends BasePageQuery {
    private static final long serialVersionUID = 1L;

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
