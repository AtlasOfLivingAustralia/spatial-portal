package au.org.emii.portal;

import java.net.Authenticator;
import java.net.PasswordAuthentication;



public class HttpAuthenticateProxy extends Authenticator {

	protected PasswordAuthentication getPasswordAuthentication() {

		
		String proxyUserName = Config.getValue("proxy_username");
		String proxyPassword = Config.getValue("proxy_password");
		return new PasswordAuthentication(proxyUserName, proxyPassword
				.toCharArray());
	}

}

