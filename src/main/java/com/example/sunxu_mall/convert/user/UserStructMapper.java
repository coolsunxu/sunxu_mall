package com.example.sunxu_mall.convert.user;

import com.example.sunxu_mall.dto.user.UserCreateDTO;
import com.example.sunxu_mall.dto.user.UserUpdateDTO;
import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.vo.user.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserStructMapper {

    UserWebEntity toEntity(UserCreateDTO createDTO);

    UserWebEntity toEntity(UserUpdateDTO updateDTO);

    UserVO toVO(UserWebEntity entity);
    
    List<UserVO> toVOList(List<UserWebEntity> entityList);
}
