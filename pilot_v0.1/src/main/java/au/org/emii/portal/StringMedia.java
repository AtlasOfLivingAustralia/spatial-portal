package au.org.emii.portal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;

import org.zkoss.util.media.Media;

public class StringMedia implements Media, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String data = null;
	
	@SuppressWarnings("unused")
	private StringMedia() {}
	
	public StringMedia(String data) {
		this.data = data;
	}
	
	public byte[] getByteData() {
		return data.getBytes();
	}

	public String getContentType() {
		return "text/plain";
	}

	public String getFormat() {
		return "plain";
	}

	public String getName() {
		return "StringMedia";
	}

	public Reader getReaderData() {
		return new StringReader(data);
	}

	public InputStream getStreamData() {
		byte currentXMLBytes[] = data.getBytes();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(currentXMLBytes);
		return byteArrayInputStream;
	}

	public String getStringData() {
		return data;
	}

	public boolean inMemory() {
		return false;
	}

	public boolean isBinary() {
		return false;
	}

}
