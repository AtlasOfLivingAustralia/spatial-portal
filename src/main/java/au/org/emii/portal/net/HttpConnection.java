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

    /**
     * Return a URL connection that times out according to the
     * net_connect_timeout and net_read_timeout entries in the
     * config file
     *
     * @param uri
     * @throws IOException
     */
    URLConnection configureURLConnection(String uri) throws IOException;

    /**
     * Return a URL connection that times out after the passed in timeouts
     *
     * @param uri            uri to connect to
     * @param connectTimeout time to wait for a connection (ms)
     * @param readtimeout    time to wait for the uri to be fully read (ms)
     * @return
     * @throws IOException
     */
    URLConnection configureURLConnection(String uri, int connectTimeout, int readtimeout) throws IOException;


    URLConnection configureURLConnectionWithAuthentication(String uri, String userName, String passWord) throws IOException;


    /**
     * Readback the raw data from a uri and return it
     *
     * @param uri
     * @return
     */
    String readRawData(String uri);

}
