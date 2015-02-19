package au.org.emii.portal.servlet;

import au.org.emii.portal.net.HttpConnection;
import au.org.emii.portal.settings.Settings;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
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
    private static final Logger LOGGER = Logger.getLogger(RemoteRequestServlet.class);
    private byte[] requestBody;
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

        //read input stream first
        requestBody = IOUtils.toString(request.getReader()).getBytes("UTF-8");

        String queryString = request.getQueryString();
        LOGGER.debug("requested: " + queryString);
        String targetUrl = request.getParameter("url");
        if (targetUrl == null) {
            LOGGER.debug("no url parameter supplied");
            outputError(response);
        } else {
            try {
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
                    String extra = rebuildParameters(request.getParameterMap());
                    if (targetUrl.contains("?") && extra.startsWith("?")) {
                        extra = "&" + extra.substring(1);
                    }
                    String target
                            = targetUrl
                            + extra;

                    LOGGER.debug("access granted to hostname : " + hostname);
                    fetchAndOutputUrl(target, response, request);
                } else {
                    outputError(response);
                    LOGGER.debug(
                            "access to " + targetUrl + " denied - host is not allowed");
                }
            } catch (MalformedURLException e) {
                LOGGER.debug("Url is malformed: " + targetUrl);
                outputError(response);
            }
        }
    }

    private String rebuildParameters(Map params) {
        StringBuilder uri = new StringBuilder();
        String delim = "?";
        for (Object o : params.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            // skip the url parameter - removal from the map is not allowed
            if (!"url".equalsIgnoreCase((String) entry.getKey())) {
                String[] value = (String[]) entry.getValue();
                try {
                    uri.append(delim).append(entry.getKey()).append("=").append(URLEncoder.encode(value[0], "UTF-8"));
                    delim = "&";
                } catch (UnsupportedEncodingException e) {
                }
            }
        }
        return uri.toString();
    }

    private void fetchAndOutputUrl(String url, HttpServletResponse response, HttpServletRequest request) {
        URLConnection con;
        InputStream is = null;
        try {
            LOGGER.debug("will request '" + url + "' from remote server");
            con = httpConnection.configureSlowURLConnection(url);


            // POST parameters
            if ("POST".equals(request.getMethod())) {
                HttpURLConnection hc = (HttpURLConnection) new URL(url).openConnection();
                if (request.getContentType() != null) {
                    hc.setRequestProperty("Content-Type", request.getContentType());
                }
                hc.setRequestMethod("POST");
                hc.setDoInput(true);
                hc.setDoOutput(true);

                if (requestBody != null && requestBody.length > 0) {
                    hc.getOutputStream().write(requestBody);
                }

                con = hc;
            }

            // force the url to be cached by adding/replaceing the cache-control header
            response.addHeader(
                    "Cache-Control",
                    "max-age=" + settings.getCacheMaxAge() + "public must-revalidate");

            // get the data...
            is = con.getInputStream();
            byte[] data = IOUtils.toByteArray(is);

            // restore the MIME type for correct handling
            response.setContentType(con.getContentType());

            // flush to client
            response.getOutputStream().write(data);

        } catch (IOException e) {
            LOGGER.debug("IO error doing remote request: " + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing stream to " + url, e);
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
        return allowedHosts.contains(hostname);

    }

    private void loadList() {
        allowedHosts.clear();
        String[] allowedHostsRaw = settings.getProxyAllowedHosts().split("\\|");
        List<String> newAllowedHosts = new ArrayList<String>();
        // now remove whitespace and store in the master list
        for (String string : allowedHostsRaw) {
            String trimmed = string.trim();
            LOGGER.debug("allowed host + " + trimmed);
            newAllowedHosts.add(trimmed);
        }
        allowedHosts = newAllowedHosts;
        updatedOn = new Date();

    }

    private void initAllowedHosts() {
        if (updatedOn == null) {
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

    @Required
    public void setHttpConnection(HttpConnection httpConnection) {
        this.httpConnection = httpConnection;
    }
}
