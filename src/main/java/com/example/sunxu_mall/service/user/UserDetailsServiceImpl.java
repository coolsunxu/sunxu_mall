package com.example.sunxu_mall.service.user;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.entity.sys.MenuEntity;
import com.example.sunxu_mall.entity.sys.RoleEntity;
import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.mapper.sys.MenuEntityMapper;
import com.example.sunxu_mall.mapper.sys.RoleEntityMapper;
import com.example.sunxu_mall.mapper.sys.UserWebEntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/24 20:55
 * @description
 */


@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserWebEntityMapper userMapper;

    public UserDetailsServiceImpl(
            UserWebEntityMapper userMapper
    ) {
        this.userMapper = userMapper;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserWebEntity userEntity = userMapper.findByUserName(username);
        if (Objects.isNull(userEntity)) {
            return null;
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>(Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
        List<String> roles = authorities.stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtUserEntity(userEntity.getId(),
                username,
                userEntity.getPassword(),
                authorities,
                roles);
    }

}
