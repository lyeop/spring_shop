package com.example.Spring_shop.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import java.io.IOException;
//인증되지 않은 사용자가 리소스 요청하면 차단 하는 클래스
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {
        // 인증되지 않은 사용자에게 401 에러를 반환합니다.
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Unauthorized");
    }

}
