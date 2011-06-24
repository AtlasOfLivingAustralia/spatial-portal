/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker inteface to turn on null checking
 * @author geoff
 */

@Target( {ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckNotNull {}
