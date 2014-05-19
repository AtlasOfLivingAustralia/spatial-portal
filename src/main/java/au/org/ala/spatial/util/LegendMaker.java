
package au.org.ala.spatial.util;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Create the images required for legends of dynamically styled layers
 * much easier to do it here than to try to send back hacked sld requests
 * to geoserver
 * <p/>
 * brendon
 */
public class LegendMaker {

    public BufferedImage singleRectImage(Color colour, int width, int height, double rectHeight, double rectWidth) {

        // Create a buffered image that supports transparency
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        Graphics2D g2d = bi.createGraphics();
        g2d.setColor(colour);
        g2d.setComposite(ac);
        Stroke s = new BasicStroke(4.0F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
        g2d.fill(new Rectangle2D.Double(0, 0, rectWidth, rectHeight));
        g2d.dispose();
        return bi;
    }

    public BufferedImage singleCircleImage(Color colour, int width, int height, double radius) {

        // Create a buffered image that supports transparency
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        Graphics2D g2d = bi.createGraphics();
        g2d.setColor(colour);
        Stroke s = new BasicStroke(4.0F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
        g2d.setComposite(ac);
        g2d.fill(new Ellipse2D.Double(10, 10, radius, radius));
        g2d.setStroke(s);
        g2d.dispose();
        return bi;
    }
}