package com.hayden.gateway.federated;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FederatedAspect {

    @Around("@annotation(cdc)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, Cdc cdc) throws Throwable {
        // TODO: serialize and send to database so that the data services can see changes provided by other services
        return joinPoint.proceed();
    }

}
