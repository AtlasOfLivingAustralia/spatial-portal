/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.aspect;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Log the value of a setter method
 * @author geoff
 */
@Aspect
public class LogSetterValueAspect {
    private Logger logger = Logger.getLogger(getClass());

    @Before("setterMethod() ")
    public void logValue(JoinPoint jp) {
        Object value = jp.getArgs()[0];

        // we don't always have the parameter name (eg if an interface was used)
        // so just coerce this value from the setter name (remove set, lowercase
        // first letter)
        String methodName = jp.getSignature().getName();

        String parameterName = methodName.replace("set", "");
        parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1, parameterName.length());

        logger.info(String.format("'%s' ==> '%s'", parameterName, value));
    }

    /**
     * Match a setter with one parameter, marked with the @LogSetterValue annotation
     */
    @Pointcut("execution(@au.org.emii.portal.aspect.LogSetterValue void set*(*))")
    public void setterMethod() {}
}
