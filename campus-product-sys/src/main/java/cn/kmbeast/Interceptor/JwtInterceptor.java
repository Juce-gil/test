package cn.kmbeast.Interceptor;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.utils.JwtUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jsonwebtoken.Claims;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Writer;

/**
 * token拦截器，做请求拦截
 * 思路：用户登录成功后，会得到后端生成的 token，前端会将token存储于本地
 * 随后的接口请求，都会在协议头带上token
 * 所有请求执行之前，都会被该拦截器拦截：token校验通过则正常放行请求，否则直接返回
 *
 * @author 【B站：程序员辰星】
 */
public class JwtInterceptor implements HandlerInterceptor {

    /**
     * 前置拦截
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param handler  chosen handler to execute, for type and/or instance evaluation
     * @return boolean true ： 放行；false拦截
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestMethod = request.getMethod();
        // 放行预检请求（主要用于 CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
            return true;
        }

        String token = request.getHeader("token");
        // 未携带 token，直接拦截
        if (token == null || token.trim().isEmpty()) {
            Result<String> error = ApiResult.error("身份认证异常，请先登录");
            response.setContentType("application/json;charset=UTF-8");
            Writer stream = response.getWriter();
            stream.write(JSONObject.toJSONString(error));
            stream.flush();
            stream.close();
            return false;
        }

        Claims claims = JwtUtil.fromToken(token);
        // 解析不成功，直接退回！访问后续资源的可能性都没有！
        if (claims == null) {
            Result<String> error = ApiResult.error("身份认证异常，请先登录");
            response.setContentType("application/json;charset=UTF-8");
            Writer stream = response.getWriter();
            // 将失败信息输出
            stream.write(JSONObject.toJSONString(error));
            stream.flush();
            stream.close();
            return false;
        }
        Integer userId = claims.get("id", Integer.class);
        Integer roleId = claims.get("role", Integer.class);
        // 将解析出来的用户ID、用户角色放置于LocalThread中，当前线程可用
        LocalThreadHolder.setUserId(userId, roleId);
        return true;
    }
}
