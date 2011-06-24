package org.ala.spatial.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import javax.imageio.ImageIO;

/**
 *
 * @author ajay
 */
public class WrapBufferedImage implements Serializable {

	static final long serialVersionUID = 6583156430853699407L;

	
    private BufferedImage im = null;

    public WrapBufferedImage() {
        super();
    }

    public BufferedImage getIm() {
        return im;
    }

    public void setIm(BufferedImage im) {
        this.im = im;
    }

    private BufferedImage fromByteArray(byte[] imagebytes) {
        try {
            if (imagebytes != null && (imagebytes.length > 0)) {
                BufferedImage im = ImageIO.read(new ByteArrayInputStream(imagebytes));
                return im;
            }
            return null;
        } catch (IOException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    private byte[] toByteArray(BufferedImage o) {
        if (o != null) {
            BufferedImage image = (BufferedImage) o;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            try {
                ImageIO.write(image, "png", baos);
            } catch (IOException e) {
                throw new IllegalStateException(e.toString());
            }
            byte[] b = baos.toByteArray();
            return b;
        }
        return new byte[0];
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        byte[] b = toByteArray(im);
        out.writeInt(b.length);
        out.write(b);
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        int length = in.readInt();
        byte[] b = new byte[length];
        in.read(b);
        im = fromByteArray(b);
    }
}
