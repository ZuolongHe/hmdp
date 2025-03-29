package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.net.HttpCookie;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

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

    // 序列化存入redis的值
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送短信验证码
     *
     * @return Result
     */
    @Override
    public Result sendCode(String phone) {
        // 1.校验手机号, 符合, 生成验证码, 不符合，返回错误信息
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("号码输入错误！");
        } else {
            // 2.生成6位数验证码
            String numbers = RandomUtil.randomNumbers(6);

            // 3.验证码保存到redis中,设置2min有效期
            stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, numbers, LOGIN_CODE_TTL, TimeUnit.MINUTES);

            // 4.返回结果
            log.info("code：{}", numbers);
            return Result.ok(numbers);
        }

    }

    /**
     * 登录功能实现
     *
     * @param loginForm
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm) {
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            return Result.fail("号码输入错误！");
        }
        // 1.校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + loginForm.getPhone());
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }
        // 2.查询用户信息
        User user = userMapper.selectByPhone(loginForm.getPhone());
        if (user == null){
            // 注册新用户信息
            user = cresteNewUser(loginForm.getPhone());
        }
        // 3.生成token存入到redis缓存
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO);
        map.put("id", String.valueOf(map.get("id")));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
        // 设置有效期30min
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    // 创建新用户
    private User cresteNewUser(String phone) {
        User user = new User();
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
        user.setPhone(phone);
        userMapper.addUser(user);
        return user;
    }


}
