package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @Title hmdp
 * @Author hzl
 * @Date 2024/12/23 21:24
 * @Description
 */
@Slf4j
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 根据sessionId获取对应的session
        // HttpSession session = request.getSession();
        // UserDTO user = (UserDTO) session.getAttribute("user");
        // 获取token
        String token = request.getHeader("authorization");
        if (token == null){
            return false;
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(token);
        UserDTO userDTO = BeanUtil.fillBeanWithMap(entries, new UserDTO(), false);
        log.info("user:{}", userDTO);
        // ThreadLocal
        if (userDTO == null){
            response.setStatus(401);
            return false;
        }
        // 存在，用户信息保存在ThreadLocal中
        UserHolder.saveUser(userDTO);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
