package bit.bitgroundspring.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(
        name = "logging.level.bit.bitgroundspring",
        havingValue = "DEBUG",
        matchIfMissing = false
)
public class MethodTracingAspect {
    
    @Around("execution(* bit.bitgroundspring..*.*(..))") // Adjust package as needed
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        long start = System.currentTimeMillis();
        
        log.debug(">>>> {} with args: {}", methodName, args);
        
        Object result = null;
        try {
            result = joinPoint.proceed(); // Execute the actual method
        } catch (Throwable ex) {
            log.error("!!!! Exception in {}: {}", methodName, ex.getMessage(), ex);
            throw ex;
        } finally {
            long end = System.currentTimeMillis();
            log.debug("<<<<️ Exiting: {} in {}ms, result: {}", methodName, (end - start), result);
        }
        return result;
    }
}