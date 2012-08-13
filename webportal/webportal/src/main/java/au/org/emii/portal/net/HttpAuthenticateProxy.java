package au.org.emii.portal.net;

import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.util.Validate;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import org.apache.log4j.Logger;

public class HttpAuthenticateProxy extends Authenticator {

    private Settings settings = null;
    private Logger logger = Logger.getLogger(getClass());

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        String proxyUserName = settings.getProxyUsername();
        String proxyPassword = settings.getProxyPassword();
        logger.debug(String.format("proxy username='%s' proxy password='%s'",proxyUserName, proxyPassword));
        PasswordAuthentication authenticator;
        if (Validate.empty(proxyUserName) && Validate.empty(proxyPassword)) {
            logger.debug("no username and password supplied for proxy - will return null authenticator");
            authenticator = null;
        } else {
            authenticator = new PasswordAuthentication(proxyUserName, proxyPassword.toCharArray());
        }
        return authenticator;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }
}

