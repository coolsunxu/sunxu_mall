package com.example.sunxu_mall.entity.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/24 20:10
 * @description
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
public class JwtUserEntity implements UserDetails {

    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private List<SimpleGrantedAuthority> authorities;
    /**
     * 角色信息
     */
    private List<String> roles;

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

