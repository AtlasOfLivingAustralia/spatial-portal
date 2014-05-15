/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test;

/**
 *
 * @author geoff
 */

import org.apache.log4j.Logger;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Just provide a base spring config file, the runner and some imports.  Although
 * the imports aren't all used in this class, they seem to help netbeans detect
 * the correct annotation to import in subclasses
 *
 * @author geoff
 */
@RunWith(SpringJUnit4ClassRunner.class)
// config file within src/test/resources
@ContextConfiguration(locations = {"/au/org/emii/portal/test/test-config.xml"})
public class AbstractTester {
    protected Logger logger = Logger.getLogger(getClass());

}
