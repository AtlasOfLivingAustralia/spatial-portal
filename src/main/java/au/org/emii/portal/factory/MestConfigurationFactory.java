/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.factory;

import au.org.emii.portal.mest.MestConfiguration;



/**
 *
 * @author geoff
 */
public interface MestConfigurationFactory {

    public MestConfiguration createInstance(au.org.emii.portal.config.xmlbeans.MestConfiguration xmlMestConfiguration);
}