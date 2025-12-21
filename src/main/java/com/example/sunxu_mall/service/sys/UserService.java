package com.example.sunxu_mall.service.sys;


import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.mapper.sys.UserWebEntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {
    private final UserWebEntityMapper userMapper;

    public UserService(UserWebEntityMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserWebEntity getUserInfo() {
        UserWebEntity rs = userMapper.selectByPrimaryKey(13L);
        return rs;
    }


}
