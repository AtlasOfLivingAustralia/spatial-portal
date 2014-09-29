/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.net;

import java.io.IOException;
import java.net.URLConnection;

/**
 * @author geoff
 */
public interface HttpConnection {

    /**
     * Return a URL connection that times out according to the
     * net_connect_slow_timeout and net_read_slow_timeout entries
     * in the config file
     *
     * @param uri to connect to
     * @return
     * @throws IOException
     */
    URLConnection configureSlowURLConnection(String uri) throws IOException;

}
