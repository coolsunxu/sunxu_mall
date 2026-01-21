package com.example.sunxu_mall.context;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author sunxu
 */

@Data
@AllArgsConstructor
public class AuditUser {
    private Long userId;
    private String userName;
}

