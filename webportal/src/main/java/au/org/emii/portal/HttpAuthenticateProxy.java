package au.org.emii.portal;

import au.org.emii.portal.config.Settings;
import java.net.Authenticator;
import java.net.PasswordAuthentication;



public class HttpAuthenticateProxy extends Authenticator {

    private Settings settings = null;

    @Override
	protected PasswordAuthentication getPasswordAuthentication() {

		
		String proxyUserName = settings.getProxyUsername();
		String proxyPassword = settings.getProxyPassword();
		return new PasswordAuthentication(proxyUserName, proxyPassword.toCharArray());
	}

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }



}

