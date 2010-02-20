package au.org.emii.portal;

import au.org.emii.portal.lang.LanguagePack;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

public class ImageTesterImpl implements ImageTester {
	private boolean readError = false;
	private boolean ogcError = false;
	private boolean incorrectMimeType = false;
	private String lastUriAttempted = null;
	private String lastErrorMessage = null;
	private Logger logger = Logger.getLogger(this.getClass());

    private LanguagePack languagePack = null;
    private LayerUtilities layerUtilities = null;
    private HttpConnection httpConnection = null;
	
	/**
	 * Test a WMS server by requesting a 2x2 px image from it.
	 * 
	 * If an error occurred, the fail message will be stored in 
	 * the lastLayerTestErrorMessage variable.
	 * @param mapLayer mapLayer instance
	 * @return true if the maplayer getmap uri poits to an image
	 * otherwise false.
	 */
	public boolean testLayer(MapLayer mapLayer) {
	
		/* request 1px square region to test map service - when handling for a layer,
		 * we expect the version, service and layers variables to already be set 
		 * by the user or the discovery program within the url
		 */
		return testImageUri(
				layerUtilities.getTestImageUri(mapLayer.getUri()),
				mapLayer.getImageFormat(),
				false
		);
	}
	
	/**
	 * Test that the passed in uri returns an image of mime-type type.
	 * If an error occurred, the fail message will be stored in 
	 * the lastLayerTestErrorMessage variable.
	 * @param uri URI of image to download
	 * @param wantedType wanted MIME type of image
	 * @param slowServer true if you want to allow the server to be slow 
	 * @return true if the test image is the correct mime type, otherwise false
	 */
	public boolean testImageUri(String uri, String wantedType, boolean slowServer) {
		logger.debug(
				"Testing image at '" + uri + "' for type '" + wantedType + "'"
		);
		boolean testOk = false;
		URLConnection con = null;
		// request the data
		try {
			lastUriAttempted = uri;
			if (slowServer) {
				con = httpConnection.configureSlowURLConnection(uri);
			}
			else {
				con = httpConnection.configureURLConnection(uri);
			}
			
			/* although we're not interested in anything other than the
			 * http header, we MUST request the file contents anyway.  If
			 * we don't, squid won't cache the request properly and the
			 * client can get 'stuck' re-requesting things 
			 */
			InputStream in = con.getInputStream();
			IOUtils.toByteArray(in);
			
			// if the mime type of the data read from the uri matches
			// mapLayer.getImageFormat(), then we're all good
			String mime = con.getContentType();
			if (mime == null) {
				// error - read was aborted (timed out) or just totally broken
				readError = true;
				lastErrorMessage = 
					"Error connecting to: " + uri + 
					" (returned no data or timed out)";
			}
			else if (mime.equalsIgnoreCase(wantedType)) {
				// all sweet
				testOk = true;

				logger.debug("layer is OK");
				
			}
			else if (mime.contains("application/vnd.ogc.se_xml")) {
				// error - an ogc exception document - find out what the problem is
				ogcError = true;
				lastErrorMessage = ServiceExceptionParser.parseAndGetReason(con.getInputStream());
				logger.debug("OGC exception message: " + lastErrorMessage);
			}
			else {
				// wrong mime type
				incorrectMimeType = true;
				lastErrorMessage = 
					"Obtained a document of type '" + mime + "' (expected '" +
					wantedType + "') from '" + uri + "'";
				logger.debug(lastErrorMessage);				
			}
		} 
		catch (IOException e) {
			// error
			readError = true;
			logger.info("IO error connecting to " + uri);
			lastErrorMessage = 
				"Error connecting to " + uri +
				" (IO error - " + e.getMessage() + ")";
		}
		return testOk;
	}
	
	/**
	 * The result of all the user's clicking through the gui to pick
	 * dates for animation, is just a uri that points to a server 
	 * generated animated gif image.
	 * 
	 * Sometimes things go astray in this process and what is supposed
	 * to be an image ends up being a text warning or an ogc exception 
	 * document.
	 * 
	 * Openlayers will detect that an image is not an image and will 
	 * render the "layer unavailable" image, so what we want to do is
	 * independently request the image that openlayers will ask for 
	 * and see if its valid.  If it isn't then we can stop the user from 
	 * loading the layer into the menu system and can give them a friendly
	 * error message.
	 * 
	 * Although it looks like this will double the load on the server
	 * for animations as the same image URI is requested twice (once at 
	 * the server end to test the image and again at the client end to
	 * display it) it actually doesn't place any extra load on the map
	 * server (ncwms/thredds) since subsequence requests will be 
	 * cached and served through the squid proxy (assuming its setup 
	 * correctly)
	 */
	public boolean testAnimationImage(MapLayer mapLayer) {
		Date start = new Date();
		
		String animationUri = layerUtilities.getAnimationUri(mapLayer);
		
		/* we wait up to whatever the slow server timeout is for animations
		 * worst case is 20 minutes at the moment (connect + read)
		 */
		boolean valid = testImageUri(animationUri, "image/gif", true);
		Date end = new Date();
		if (! valid) {
			logger.debug("testImage() FAILED for " + animationUri + " REASON: " + lastErrorMessage);
		}
		long time = end.getTime() - start.getTime();
		logger.debug("testImage() took " + time + " ms to complete");
		return valid;
	}

	public boolean isReadError() {
		return readError;
	}

	public boolean isOgcError() {
		return ogcError;
	}

	public String getLastUriAttempted() {
		return lastUriAttempted;
	}
	
	public String getErrorMessage() {
		return lastErrorMessage;
	}
	
	public String getErrorMessageSimple() {
		String message;
		if (readError) {
			message = languagePack.getLang("read_error_message");
		}
		else if (ogcError) {
			message = languagePack.getLang("ogc_error_message");
		}
		else if (incorrectMimeType) {
			message = languagePack.getLang("incorrect_mime_type_message");
		}
		else {
			// should never happen ;-)
			message = languagePack.getLang("unknown_error");
		}
		return message;
	}

	public boolean isIncorrectMimeType() {
		return incorrectMimeType;
	}

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    @Required
    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }

    public LayerUtilities getLayerUtilities() {
        return layerUtilities;
    }

    @Required
    public void setLayerUtilities(LayerUtilities layerUtilities) {
        this.layerUtilities = layerUtilities;
    }

    public HttpConnection getHttpConnection() {
        return httpConnection;
    }

    @Autowired
    public void setHttpConnection(HttpConnection httpConnection) {
        this.httpConnection = httpConnection;
    }



}
