package com.example.sunxu_mall.service.user;

import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.sys.UserWebEntityMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.sunxu_mall.errorcode.ErrorCode.USER_NOT_EXIST;

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
            throw new BusinessException(USER_NOT_EXIST.getCode(),"User not found");
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
