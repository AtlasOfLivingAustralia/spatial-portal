/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.factory;

import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.wms.DiscoveryProcessor;

/**
 *
 * @author ajay
 */
public abstract class DiscoveryProcessorFactoryImpl implements DiscoveryProcessorFactory {

    //private Logger logger = Logger.getLogger(getClass());

    @Override
    public DiscoveryProcessor getDiscoveryProcessorForWMSVersion(int version) {
        DiscoveryProcessor dp = null;
        switch (version) {
            case LayerUtilities.WMS_1_3_0:
                dp = createWmsSupport_1_3_0();
                break;
            case LayerUtilities.WMS_1_1_1:
                dp = createWmsSupport_1_1_1();
                break;
            case LayerUtilities.WMS_1_1_0:
                dp = createWmsSupport_1_1_0();
                break;
            case LayerUtilities.WMS_1_0_0:
                dp = createWmsSupport_1_0_0();
                break;
            case LayerUtilities.NCWMS:
                dp = createNcWmsSupport();
                break;
            case LayerUtilities.THREDDS:
                dp = createThreddsSupport();
                break;
            default:
                //logger.warn("no support for discovery type - " + version + " yet!");
                System.out.println("no support for discovery type - " + version + " yet!");
        }

        return dp;
    }

    public abstract DiscoveryProcessor createWmsSupport_1_3_0();

    public abstract DiscoveryProcessor createWmsSupport_1_1_1();

    public abstract DiscoveryProcessor createWmsSupport_1_1_0();

    public abstract DiscoveryProcessor createWmsSupport_1_0_0();

    public abstract DiscoveryProcessor createNcWmsSupport();

    public abstract DiscoveryProcessor createThreddsSupport();
}