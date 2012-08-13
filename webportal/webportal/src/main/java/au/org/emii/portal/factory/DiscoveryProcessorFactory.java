/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.factory;

import au.org.emii.portal.wms.DiscoveryProcessor;

/**
 *
 * @author ajay
 */
public interface DiscoveryProcessorFactory {
    public DiscoveryProcessor getDiscoveryProcessorForWMSVersion(int version);
}
