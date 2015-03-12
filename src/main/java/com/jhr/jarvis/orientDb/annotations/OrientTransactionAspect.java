package com.jhr.jarvis.orientDb.annotations;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.jhr.jarvis.service.OrientDbService;


@DependsOn({"springApplicationContextHolder", "orientDbService"})
@Configuration
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrientTransactionAspect {
	
	@Autowired
	private OrientDbService orientDbService;
	
	@Around("@annotation(OrientTransaction)")
	public Object invoke(ProceedingJoinPoint pjp) throws Throwable {
 
		// same with MethodBeforeAdvice
		System.out.println("HijackAroundMethod : Before method hijacked!");
 
		OrientDb orientDb = null;
		
		try {
			
			MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
			
			orientDb = new OrientDb(orientDbService.getFactory().getTx());
			
			Object[] args = setOrientGraphIntoArgs(methodSignature.getMethod(), pjp.getArgs(), orientDb);
			
			// proceed to original method call
			Object result = pjp.proceed(args);
 
			// same with AfterReturningAdvice
			System.out.println("HijackAroundMethod : Before after hijacked!");
 
			return result;
 
		} catch (Exception e) {
			
			if (orientDb.getGraph() != null) {
				orientDb.getGraph().rollback();
			}
			
			// same with ThrowsAdvice
			System.out.println("HijackAroundMethod : Throw exception hijacked!");
			e.printStackTrace();
			throw e;
		} finally {
			if (orientDb.getGraph() != null) {
				orientDb.getGraph().commit();
			}
		}
	}
	
    private Object[] setOrientGraphIntoArgs(Method method, Object[] args, OrientDb orientDbWithActiveTransaction) {
        for(int i = 0; i < method.getParameterTypes().length; i++) {
            Class<?> parameterType = method.getParameterTypes()[i];
            if(parameterType.getName().equals(OrientDb.class.getName())) {
                args[i] = orientDbWithActiveTransaction;
                break;
            }
        }
        
        return args;
    }
	
}
