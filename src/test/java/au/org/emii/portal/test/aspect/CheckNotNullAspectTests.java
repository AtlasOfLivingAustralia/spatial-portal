/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.aspect;

import au.org.emii.portal.config.Settings;
import au.org.emii.portal.test.AbstractTester;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.Assert.*;
/**
 *
 * @author geoff
 */
public class CheckNotNullAspectTests extends AbstractTester {

    @Autowired
    private Settings settings = null;
    private final static String TEST_STRING = "XXXXXX";

    @Test
    public void testSettingNull() {
        try {
            settings.setProxyHost(null);
            fail("NPE not raised");
        } catch (NullPointerException e) {
            logger.info(e);
            assertTrue(true);
        }
    }

    @Test
    public void testSettingNotNull() {
        try {
            settings.setProxyHost(TEST_STRING);
            assertEquals("value for get doesn't match set", TEST_STRING, settings.getProxyHost());
        } catch (NullPointerException e) {
            fail("incorrect NPE generated");
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    
}
