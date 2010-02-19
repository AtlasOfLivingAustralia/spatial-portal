package au.org.emii.portal;

import au.org.emii.portal.config.xmlbeans.Discovery;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public abstract class WMSSupport implements DiscoveryProcessor {

	
	protected String lastErrorMessage = null;
	protected Logger logger = Logger.getLogger(this.getClass());	
	protected float opacity = 0.0f;
	protected String baseUri = null;
	protected String imageFormat = null;
	protected String id = null;
	protected String description = null;
	protected boolean broken = false;
	protected String discoveryName = null;
	protected boolean parseError = false;
	protected boolean readError = false;
	protected boolean quiet = false;
	protected boolean queryableDisabled = false;
	protected boolean cache = false;

	/**
	 * Get the log4j level to use when discovering services.  If
	 * we have been put in quiet mode, only log to the debug 
	 * console.  This prevents receiving hundreds of error messages
	 * each time a user attempts to add layers from a broken uri.
	 * @return
	 */
	public Priority getLogLevel() {
		Priority priority;
		if (quiet) {
			priority = Level.DEBUG;
		}
		else {
			priority = Level.ERROR;
		}
		
		return priority;
	}

        @Override
	public String getLastErrorMessage() {
		return lastErrorMessage;
	}

	
	public static String sequenceFragment(Sequence sequence) {
		String sequenceString;
		if (sequence.getValue() > 0) {
			sequenceString = ":" + sequence.getValue();
		}
		else {
			sequenceString = "";
		}
		return sequenceString;
	}
		
        @Override
	public boolean isParseError() {
		return parseError;
	}

        @Override
	public boolean isReadError() {
		return readError;
	}


        /**
         * we managed to process the service but something somewhere is broken
         * so we return null to the caller.  If the broken flag gets set, it
         * could be cause by an unsupported image format - check for version
         * specific implementations.
         *
         * Setting the broken flag doesn't stop processing, and its likely that
         * there will be a catastrophic failure from other, worse errors that
         * will send flow directly to exception handling instead of here...
         *
         * Check that no errors were raised - if they were, return null, otherwise
         * return the maplayer back.  This lets us skip adding broken layers
         */
        protected MapLayer checkErrorFlags(Discovery discovery, MapLayer mapLayer) {
            MapLayer checked;
            if ((broken || parseError || readError)) {
                    checked = null;
                    logger.log(
                        getLogLevel(),
                        "Disabling discovery " + discovery.getId() + " at '" +
                        discovery.getUri() + "' - because: " + lastErrorMessage
                    );
            } else {
                checked = mapLayer;
            }
            return checked;
        }
}
