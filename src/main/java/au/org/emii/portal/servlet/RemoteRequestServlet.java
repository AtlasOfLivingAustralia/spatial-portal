package au.org.emii.portal.servlet;

import au.org.emii.portal.config.ConfigurationLoaderStage1Impl;
import au.org.emii.portal.net.HttpConnection;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.web.ApplicationInit;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 'Magically' cache requests to remote servers. Here's how:
 * <p/>
 * Tomcat is hosted behind apache which is behind squid. Therefore GET urls are
 * intecepted by squid and cached as required. By using the university proxy and
 * 'tweaking' the http headers, we can get squid to cache stuff from other
 * people's sites:
 * <p/>
 * e.g, user requests a map tile through this servlet:
 * http://imos.aodn.org.au/RemoteRequest?url=http://blah.org/wms?tile=ABC
 * <p/>
 * And this will be automatically cached.
 * <p/>
 * To protect against creating an open proxy, we will only allow:
 * <p/>
 * A) requests to be sent to hosts whitelisted in the proxyAllowedHosts section
 * of the config file
 * <p/>
 * - And -
 * <p/>
 * B) 'known wms servers' which are those servers that have been added by the
 * user and have been autodetected as valid WMS servers.
 * <p/>
 * - And -
 * <p/>
 * C) Act as a proxy for GetFeatureInfo requests to get around the browser
 * server of origin policy. This replaces the proxy.cgi script
 * <p/>
 * B) will be added later as part of the work for version 2, for now only
 * support for A) will be added which will allow for acceleration of IVECs
 * THREDDS server which has been impossible so far because of the irritating and
 * pointless proxying setup at UTAS
 * <p/>
 * Servlet implementation class RemoteRequest
 */
public class RemoteRequestServlet implements HttpRequestHandler {

    private static final long serialVersionUID = 1L;

    /**
     * Logger instance
     */
    private static Logger logger = Logger.getLogger(RemoteRequestServlet.class);
    private List<String> allowedHosts = new ArrayList<String>();
    private Date updatedOn = null;

    private Settings settings = null;
    private HttpConnection httpConnection = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public RemoteRequestServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String queryString = request.getQueryString();
        logger.debug("requested: " + queryString);
        String targetUrl = request.getParameter(settings.getCacheParameter());
        if (targetUrl == null) {
            logger.debug("no url parameter supplied");
            outputError(response);
        } else {
            try {
                URL url = new URL(targetUrl);
                String hostname = url.getHost();

                // check hostname on whitelist
                initAllowedHosts(request);

                // proxying should have already been setup in the System
                // class by ConfigurationLoader
                if (allowed(hostname)) {
                    // ok - we are allowed to access this host, now
                    // we can grab the other parameters and append
                    // them to the url
                    String target
                            = targetUrl
                            + rebuildParameters(request.getParameterMap());//LayerUtilities.stripParameter(CACHE_PARAMETER, queryString);

                    logger.debug("access granted to hostname : " + hostname);
                    fetchAndOutputUrl(target, response);
                } else {
                    outputError(response);
                    logger.debug(
                            "access to " + targetUrl + " denied - host is not allowed");
                }
            } catch (MalformedURLException e) {
                logger.debug("Url is malformed: " + targetUrl);
                outputError(response);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "WMI_WRONG_MAP_ITERATOR")
    private String rebuildParameters(Map params) {
        StringBuilder uri = new StringBuilder();
        String delim = "?";
        for (Object key : params.keySet()) {
            // skip the url parameter - removal from the map is not allowed
            if (!((String) key).equalsIgnoreCase(settings.getCacheParameter())) {
                String[] value = (String[]) params.get(key);
                uri.append(delim).append(key).append("=").append(value[0]);
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
            con = httpConnection.configureSlowURLConnection(url);

            // restore the MIME type for correct handling
            response.setContentType(con.getContentType());

            // force the url to be cached by adding/replaceing the cache-control header
            response.addHeader(
                    "Cache-Control",
                    "max-age=" + settings.getCacheMaxAge() + "public must-revalidate");

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
                    logger.error("Error closing stream to " + url, e);
                }
            }
        }
    }

    /**
     * Check if we are allowed to access hostname
     *
     * @param hostname
     * @return true if we are allowed (it's whitelisted) otherwise false
     */
    private boolean allowed(String hostname) {
        return (allowedHosts.contains(hostname));

    }

    private void loadList() {
        allowedHosts.clear();
        String[] allowedHostsRaw = settings.getProxyAllowedHosts().split("\\|");
        List<String> newAllowedHosts = new ArrayList<String>();
        // now remove whitespace and store in the master list
        for (String string : allowedHostsRaw) {
            String trimmed = string.trim();
            logger.debug("allowed host + " + trimmed);
            newAllowedHosts.add(trimmed);
        }
        allowedHosts = newAllowedHosts;
        updatedOn = new Date();

    }

    private void initAllowedHosts(HttpServletRequest request) {
        ServletContext servletContext = request.getSession().getServletContext();
        ConfigurationLoaderStage1Impl stage1
                = (ConfigurationLoaderStage1Impl) servletContext.getAttribute(ApplicationInit.CONFIGURATION_LOADER_ATTRIBUTE);
        Date portalLastReloaded = stage1.getLastReloaded();
        if ((updatedOn == null || updatedOn.getTime() < portalLastReloaded.getTime())) {
            loadList();
        }
    }

    private void outputError(HttpServletResponse response) throws IOException {
        response.getWriter().write("NOT ALLOWED");
    }

    public Settings getSettings() {
        return settings;
    }

    @Required
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public HttpConnection getHttpConnection() {
        return httpConnection;
    }

    @Required
    public void setHttpConnection(HttpConnection httpConnection) {
        this.httpConnection = httpConnection;
    }
}
