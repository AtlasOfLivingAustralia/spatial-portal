/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.factory;

import au.org.emii.portal.mest.MestConfiguration;
import org.apache.log4j.Logger;

/**
 *
 * @author geoff
 */
public abstract class MestConfigurationFactoryImpl implements MestConfigurationFactory {

    private Logger logger = Logger.getLogger(getClass());

    @Override
    public MestConfiguration createInstance(au.org.emii.portal.config.xmlbeans.MestConfiguration xmlMestConfiguration) {
        MestConfiguration mestConfiguration = null;
        try {
            if (xmlMestConfiguration.getDisabled()) {
                logger.info(String.format(
                        "skipping loading mest configuration '%s' because it is disabled",
                        xmlMestConfiguration.getId()));
            } else {
                mestConfiguration = createInstance();
                // id, name, description...
                mestConfiguration.setDescription(xmlMestConfiguration.getDescription());
                mestConfiguration.setId(xmlMestConfiguration.getId());
                mestConfiguration.setName(xmlMestConfiguration.getName());
                mestConfiguration.setServicePathChangePassword(xmlMestConfiguration.getServicePathChangePassword());
                mestConfiguration.setServicePathKeyword(xmlMestConfiguration.getServicePathKeyword());
                mestConfiguration.setServicePathLogin(xmlMestConfiguration.getServicePathLogin());
                mestConfiguration.setServicePathResetPassword(xmlMestConfiguration.getServicePathResetPassword());
                mestConfiguration.setServicePathSelfRegistration(xmlMestConfiguration.getServicePathSelfRegistration());
                mestConfiguration.setServicePathUserInfo(xmlMestConfiguration.getServicePathUserInfo());
                mestConfiguration.setTokenDuplicateUser(xmlMestConfiguration.getTokenDuplicateUser());
                mestConfiguration.setTokenExpiredChangeKey(xmlMestConfiguration.getTokenExpiredChangeKey());
                mestConfiguration.setTokenIncorrectLogin(xmlMestConfiguration.getTokenIncorrectLogin());
                mestConfiguration.setTokenInvalidUser(xmlMestConfiguration.getTokenInvalidUser());
                mestConfiguration.setVersion(xmlMestConfiguration.getVersion());
            }
        } catch (NullPointerException e) {
            // FIXME: pretty up message
            logger.error("missing field in mest configuration section: " + e.getMessage());
            mestConfiguration = null;
        }
        return mestConfiguration;
    }

    public abstract MestConfiguration createInstance();
}
