package au.org.emii.portal.net;

import au.org.emii.portal.settings.Settings;
import org.apache.log4j.Logger;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Dead simple class to configure an InputStream based on a URI, this is so
 * that consumers have things likes timeouts set correctly and consistently
 *
 * @author geoff
 */
public class HttpConnectionImpl implements HttpConnection {
    private static final Logger LOGGER = Logger.getLogger(HttpConnectionImpl.class);
    private Settings settings = null;

    /**
     * Return a URL connection that times out according to the
     * net_connect_slow_timeout and net_read_slow_timeout entries
     * in the config file
     *
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
     *
     * @param uri            uri to connect to
     * @param connectTimeout time to wait for a connection (ms)
     * @param readtimeout    time to wait for the uri to be fully read (ms)
     * @return
     * @throws IOException
     */
    private URLConnection configureURLConnection(String uri, int connectTimeout, int readtimeout) throws IOException {
        URL url = new URL(uri);
        URLConnection con = url.openConnection();
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readtimeout);
        return con;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }
}

/*
 * BASE64 encoding encodes 3 bytes into 4 characters.
 * |11111122|22223333|33444444| Each set of 6 bits is encoded according to the
 * toBase64 map. If the number of input bytes is not a multiple of 3, then the
 * last group of 4 characters is padded with one or two = signs. Each output
 * line is at most 76 characters.
 */

class Base64OutputStream extends FilterOutputStream {
    private static char[] toBase64 = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
            'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
            'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', '+', '/'};
    private int col = 0;
    private int i = 0;
    private int[] inbuf = new int[3];

    public Base64OutputStream(OutputStream out) {
        super(out);
    }

    public void write(int c) throws IOException {
        inbuf[i] = c;
        i++;
        if (i == 3) {
            super.write(toBase64[(inbuf[0] & 0xFC) >> 2]);
            super.write(toBase64[((inbuf[0] & 0x03) << 4)
                    | ((inbuf[1] & 0xF0) >> 4)]);
            super.write(toBase64[((inbuf[1] & 0x0F) << 2)
                    | ((inbuf[2] & 0xC0) >> 6)]);
            super.write(toBase64[inbuf[2] & 0x3F]);
            col += 4;
            i = 0;
            if (col >= 76) {
                super.write('\n');
                col = 0;
            }
        }
    }

    public void flush() throws IOException {
        if (i == 1) {
            super.write(toBase64[(inbuf[0] & 0xFC) >> 2]);
            super.write(toBase64[(inbuf[0] & 0x03) << 4]);
            super.write('=');
            super.write('=');
        } else if (i == 2) {
            super.write(toBase64[(inbuf[0] & 0xFC) >> 2]);
            super.write(toBase64[((inbuf[0] & 0x03) << 4)
                    | ((inbuf[1] & 0xF0) >> 4)]);
            super.write(toBase64[(inbuf[1] & 0x0F) << 2]);
            super.write('=');
        }
    }
}



