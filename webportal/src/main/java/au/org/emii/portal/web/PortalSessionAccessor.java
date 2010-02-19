/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.web;

import au.org.emii.portal.PortalSession;
import au.org.emii.portal.SearchCatalogue;
import javax.servlet.ServletContext;

/**
 * Provide access to the PortalSession class in Application Scope
 * @author geoff
 */
public class PortalSessionAccessor {

    private static ServletContext servletContext = null;

    public static ServletContext getServletContext() {
        return servletContext;
    }

    public static void setServletContext(ServletContext servletContext) {
        PortalSessionAccessor.servletContext = servletContext;
    }

    public static PortalSession getMasterPortalSession() {
        return (servletContext == null) ? null : (PortalSession) servletContext.getAttribute("masterPortalSession");
    }

}
