package au.org.emii.portal;

import au.org.emii.portal.config.Settings;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Dead simple class to configure an InputStream based on a URI, this is so 
 * that consumers have things likes timeouts set correctly and consistently
 * @author geoff
 *
 */
public class HttpConnectionImpl implements HttpConnection {
	private Logger logger = Logger.getLogger(getClass());
    private Settings settings = null;
	
	/**
	 * Return a URL connection that times out according to the 
	 * net_connect_timeout and net_read_timeout entries in the 
	 * config file
	 * @param uri
	 * @throws IOException 
	 */
	public URLConnection configureURLConnection(String uri) throws IOException {
		return configureURLConnection(
				uri, 
				settings.getNetConnectTimeout(),
				settings.getNetReadTimeout()
		);
	}

	/**
	 * Return a URL connection that times out according to the 
	 * net_connect_slow_timeout and net_read_slow_timeout entries 
	 * in the config file
	 * @param uri to connect to
	 * @return
	 * @throws IOException
	 */
	public URLConnection configureSlowURLConnection(String uri) throws IOException {
		return configureURLConnection(
				uri, 
				settings.getNetConnectSlowTimeout(),
				settings.getNetReadSlowTimeout()
		);
	}

	/**
	 * Return a URL connection that times out after the passed in timeouts 
	 * @param uri uri to connect to
	 * @param connectTimeout time to wait for a connection (ms)
	 * @param readtimeout time to wait for the uri to be fully read (ms)
	 * @return 
	 * @throws IOException
	 */
	public URLConnection configureURLConnection(String uri, int connectTimeout, int readtimeout) 
																throws IOException {
		URL url = new URL(uri);
		URLConnection con = url.openConnection();
		con.setConnectTimeout(connectTimeout);
		con.setReadTimeout(readtimeout);
		return con;
	}
	
	/**
	 * Readback the raw data from a uri and return it 
	 * @param uri
	 * @return
	 */
	public String readRawData(String uri) {
		String raw;
		InputStream in = null;
		URLConnection con = null;
		try {
			con = configureURLConnection(uri);
			in = con.getInputStream();
			raw = IOUtils.toString(in);
		} 
		catch (IOException e) {
                        // for 404 errors, the message will be the requested url
			logger.debug(
					"IO error (" + e.getMessage() + ") reading raw " +
					"data from URI: " + uri
			);
			raw = null;
			if (in != null) {
				try { in.close(); } catch (IOException ex) { logger.debug(ex);}
			}
			
			// httpURLConnection also covers https connections
			if (con instanceof HttpURLConnection) {
				logger.debug("attempting to read error stream");
				HttpURLConnection httpCon = (HttpURLConnection) con;
				in = httpCon.getErrorStream();
				try {
					if (in != null) {
						raw = IOUtils.toString(in);
					}
				} 
				catch (IOException ex) {logger.debug(ex);}
				
			}
			else {
				logger.debug("leaving readRawData without getting error stream");
			}
		}
		finally {
			IOUtils.closeQuietly(in);
		}
		return raw;
	}

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }


}
