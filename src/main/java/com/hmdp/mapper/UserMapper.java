package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    User selectByPhone(String phone);

    // 增加新用户
    Integer addUser(User user);
}
