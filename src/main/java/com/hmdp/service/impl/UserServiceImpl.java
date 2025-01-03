package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author hzl
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 发送短信验证码
     * @return Result
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号, 符合, 生成验证码, 不符合，返回错误信息
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("号码输入错误！");
        }else {
            // 2. 生成6位数验证码
            String numbers = RandomUtil.randomNumbers(6);
            // 3. 保存到session
            session.setAttribute("code", numbers);
            // 4. 返回结果
            log.info("code：{}", numbers);
            return Result.ok(numbers);
        }

    }

    /**
     * 登录功能实现
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("号码输入错误！");
        }
        // 1. 校验验证码
        if (loginForm.getCode().equals(session.getAttribute("code"))){
            // 校验一致，根据手机号查询用户
            User user = userMapper.selectByPhone(loginForm.getPhone());
            // 检查用户是否存在
            if (user != null){
                // 若不为空，则保存到session,返回信息
                session.setAttribute("user", user);
                return Result.ok();
            }else {
                // 若为空，创建于新用户，保存到session，返回信息
                User newUser = cresteNewUser(loginForm.getPhone());
                userMapper.addUser(newUser);
                log.info("user:{}", newUser);
                session.setAttribute("user", newUser);
                return Result.ok();
            }
        }else {
            // 校验不一致
            return Result.fail("验证码输入错误！");
        }
    }

    // 创建新用户
    private User cresteNewUser(String phone){
        User user = new User();
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
        user.setPhone(phone);
        return user;
    }


}
