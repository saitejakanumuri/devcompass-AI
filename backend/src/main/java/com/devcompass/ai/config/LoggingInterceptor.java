package com.devcompass.ai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        
        log.info("[HTTP Request] --> {} {} from IP: {}", 
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
                
        return true; // continue down the chain
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // This runs after the controller finishes successfully, but before the view is rendered (not often used in REST API)
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long elapsedTime = System.currentTimeMillis() - startTime;

        if (ex != null) {
            log.error("[HTTP Response] <-- {} {} completed in {} ms with Exception: {}", 
                    request.getMethod(), request.getRequestURI(), elapsedTime, ex.getMessage());
        } else {
            log.info("[HTTP Response] <-- {} {} completed in {} ms with Status: {}", 
                    request.getMethod(), request.getRequestURI(), elapsedTime, response.getStatus());
        }
    }
}
