package com.ponmma.ss.cacheremove;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Set;

@Aspect
@Component
@Slf4j
public class CacheRemoveAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @AfterReturning("@annotation(com.ponmma.ss.cacheremove.CacheRemove)")
    public void remove(JoinPoint point) {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        CacheRemove cacheRemove = method.getAnnotation(CacheRemove.class);
        String[] keys = cacheRemove.value();
        for (String key : keys) {
            if (key.contains("#"))
                key = parseKey(key, method, point.getArgs());

            Set<String> deleteKeys = stringRedisTemplate.keys(key);
            stringRedisTemplate.delete(deleteKeys);
            log.info("cache key: " + key + " deleted");
        }
    }

    /**
     * parseKey from SPEL
     */
    private String parseKey(String key, Method method, Object[] args) {
        LocalVariableTableParameterNameDiscoverer u =
                new LocalVariableTableParameterNameDiscoverer();
        String[] paraNameArr = u.getParameterNames(method);

        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        for (int i = 0; i < paraNameArr.length; i++) {
            context.setVariable(paraNameArr[i], args[i]);
        }
        return parser.parseExpression(key).getValue(context, String.class);
    }
}