package au.org.emii.portal.servlet;

import au.org.emii.portal.HttpConnection;
import au.org.emii.portal.config.ConfigurationLoader;
import au.org.emii.portal.config.Config;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * 'Magically' cache requests to remote servers.  Here's how:
 * 
 * Tomcat is hosted behind apache which is behind squid.  Therefore
 * GET urls are intecepted by squid and cached as required.  By
 * using the university proxy and 'tweaking' the http headers, we
 * can get squid to cache stuff from other people's sites:
 * 
 * e.g, user requests a map tile through this servlet:
 * http://imos.aodn.org.au/RemoteRequest?url=http://blah.org/wms?tile=ABC
 * 
 * And this will be automatically cached.
 * 
 * To protect against creating an open proxy, we will only allow:
 * 
 * A) requests to be sent to hosts whitelisted in the proxyAllowedHosts 
 * section of the config file 
 * 
 * - And - 
 * 
 * B) 'known wms servers' which are those servers that have 
 * been added by the user and have been autodetected as valid
 * WMS servers.
 * 
 * - And -
 * 
 * C) Act as a proxy for GetFeatureInfo requests to get around
 * the browser server of origin policy.  This replaces the
 * proxy.cgi script
 * 
 * B) will be added later as part of the work for version 2, for now
 * only support for A) will be added which will allow for acceleration 
 * of IVECs THREDDS server which has been impossible so far because
 * of the irritating and pointless proxying setup at UTAS
 * 
 * Servlet implementation class RemoteRequest
 */
public class RemoteRequestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    /**
     * Logger instance
     */
    private Logger logger = Logger.getLogger(this.getClass());
    private static final String CACHE_PARAMETER = Config.getValue("cache_parameter");
    private static final List<String> allowedHosts = new ArrayList<String>();
    private boolean updating = false;
    private static Date updatedOn = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public RemoteRequestServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String queryString = request.getQueryString();
        logger.debug("requested: " + queryString);
        String targetUrl = request.getParameter(CACHE_PARAMETER);
        if (targetUrl == null) {
            logger.debug("no url parameter supplied");
            outputError(response);
        } else {
            URL url = new URL(targetUrl);
            String hostname = url.getHost();

            // check hostname on whitelist
            initAllowedHosts();

            // proxying should have already been setup in the System
            // class by ConfigurationLoader
            if (allowed(hostname)) {
                // ok - we are allowed to access this host, now
                // we can grab the other parameters and append
                // them to the url
                String target =
                        targetUrl
                        + rebuildParameters(request.getParameterMap());//LayerUtilities.stripParameter(CACHE_PARAMETER, queryString);

                logger.debug("access to hostname : " + hostname);
                fetchAndOutputUrl(target, response);
            } else {
                outputError(response);
                logger.debug(
                        "access to " + targetUrl + " denied - host is not allowed");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="WMI_WRONG_MAP_ITERATOR")
    private String rebuildParameters(Map params) {
        StringBuffer uri = new StringBuffer();
        String delim = "?";
        for (Object key : params.keySet()) {
            // skip the url parameter - removal from the map is not allowed
            if (!((String) key).equalsIgnoreCase(Config.getValue("cache_parameter"))) {
                String[] value = (String[]) params.get(key);
                uri.append(delim + key + "=" + value[0]);
                delim = "&";
            }
        }
        return uri.toString();
    }

    private void fetchAndOutputUrl(String url, HttpServletResponse response) {
        URLConnection con = null;
        InputStream is = null;
        try {
            logger.debug("will request '" + url + "' from remote server");
            con = HttpConnection.configureSlowURLConnection(url);

            // restore the MIME type for correct handling
            response.setContentType(con.getContentType());

            // force the url to be cached by adding/replaceing the cache-control header
            response.addHeader(
                    "Cache-Control",
                    "max-age=" + Config.getValue("cache_max_age") + "public must-revalidate");

            // get the data...
            is = con.getInputStream();
            byte[] data = IOUtils.toByteArray(is);

            // flush to client;
            response.getOutputStream().write(data);

        } catch (IOException e) {
            logger.debug("IO error doing remote request: " + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Check if we are allowed to access hostname
     * @param hostname
     * @return true if we are allowed (it's whitelisted) otherwise false
     */
    private boolean allowed(String hostname) {
        return (allowedHosts.contains(hostname)) ? true : false;

    }

    private void loadList() {
        synchronized (allowedHosts) {
            if (!updating) {
                updating = true;
                allowedHosts.clear();
                String list = Config.getValue("proxy_allowed_hosts");

                // nuke '' the split on ','
                list = list.replaceAll("'", "");
                String[] allowedHostsRaw = list.split(",");
                // now remove whitespace and store in the master list
                for (String string : allowedHostsRaw) {
                    String trimmed = string.trim();
                    logger.debug("allowed host + " + trimmed);
                    allowedHosts.add(trimmed);
                }
                updatedOn = new Date();
                updating = false;
                allowedHosts.notifyAll();
            }
        }
    }

    private void initAllowedHosts() {

        if ((updatedOn == null || new Date().getTime() - updatedOn.getTime() > ConfigurationLoader.rereadInterval) && !updating) {
            loadList();
        } else {
            synchronized (allowedHosts) {
                while (updating) {
                    try {
                        allowedHosts.wait();
                    } catch (InterruptedException ex) {
                    }

                }
            }
        }
    }

    private void outputError(HttpServletResponse response) throws IOException {
        response.getWriter().write("NOT ALLOWED");
    }

    /**
     * You must only make GET requests to inherit the magic
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        outputError(response);
    }
}
