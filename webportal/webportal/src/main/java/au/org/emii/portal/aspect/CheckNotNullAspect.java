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
import org.aspectj.lang.reflect.CodeSignature;

/**
 * Check passed in parameter for nullness - throw an NPE if it's null
 * @author geoff
 */
@Aspect
public class CheckNotNullAspect {

    private Logger logger = Logger.getLogger(getClass());

    @Before("setterMethod() ")
    public void checkNull(JoinPoint jp) {
        Object value = jp.getArgs()[0];
        if (value == null) {
            // try and find the param name
            CodeSignature codeSignature = (CodeSignature) jp.getSignature();

            // grab the parameter name - this may or may not exist:
            // * Not available if you compiled without the -g option to javac
            // * Also not available if the advised method is implementing an interface
            //   because java does NOT store parameter names for interfaces - ever!!!
            //   have a look with 'strings' if you don't belive me ;-)
            String paramName = (codeSignature.getParameterNames() == null) ?
                "arg0" :
                codeSignature.getParameterNames()[0];

            // grab the parameter type - this must always exist
            String paramType = codeSignature.getParameterTypes()[0].getName();

            throw new NullPointerException(
                String.format(
                    "%s.%s(%s::%s == null)",
                    jp.getTarget().getClass().getName(),  // class name
                    jp.getSignature().getName(),          // method name
                    paramType,                            // parameter type
                    paramName)                            // parameter name
            );
        }
    }

    /**
     * Match a setter with one parameter, marked with the @CheckNotNull annotation
     */
    @Pointcut("execution(@au.org.emii.portal.aspect.CheckNotNull void set*(*))")
    public void setterMethod() {}
}
