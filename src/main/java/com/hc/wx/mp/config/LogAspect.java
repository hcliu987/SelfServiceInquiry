package com.hc.wx.mp.config;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogAspect {
    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    @Around("execution(* com.hc.wx.mp.service..*(..)) || execution(* com.hc.wx.mp.controller..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long time = System.currentTimeMillis() - start;
            logger.info("方法[{}]执行完成，耗时{}ms", method, time);
            return result;
        } catch (Throwable t) {
            long time = System.currentTimeMillis() - start;
            logger.error("方法[{}]异常，耗时{}ms，异常：{}", method, time, t.getMessage());
            throw t;
        }
    }
} 