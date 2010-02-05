package au.org.emii.portal;

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
}
