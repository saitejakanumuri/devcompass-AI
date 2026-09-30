package com.devcompass.ai.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Global interceptor using Spring AOP to log controller requests and responses.
 * Since this is an Aspect wrapped around Spring Beans (Controllers), 
 * it runs AFTER the security filters have successfully let the request through.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // Target all methods within classes annotated with @RestController or inside the controller package
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        log.info("[API Request] --> {}.{}() with arguments: {}", 
            className, methodName, Arrays.toString(joinPoint.getArgs()));
            
        long startTime = System.currentTimeMillis();
        
        try {
            // Proceed to actual controller method
            Object result = joinPoint.proceed();
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            String resultStr = String.valueOf(result);
            if (resultStr.length() > 200) {
                resultStr = resultStr.substring(0, 200) + "... [truncated, total length: " + resultStr.length() + "]";
            }
            
            log.info("[API Response] <-- {}.{}() executed in {} ms. Returned: {}", 
                className, methodName, elapsedTime, resultStr);
                
            return result;
        } catch (IllegalArgumentException e) {
            log.error("[API Error] Illegal argument in {}.{}(): {}", 
                className, methodName, Arrays.toString(joinPoint.getArgs()));
            throw e;
        } catch (Throwable e) {
            log.error("[API Error] Exception in {}.{}(): {}", 
                className, methodName, e.getMessage());
            throw e;
        }
    }
}
