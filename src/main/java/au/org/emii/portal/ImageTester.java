/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal;

import au.org.emii.portal.lang.LanguagePack;

/**
 *
 * @author geoff
 */
public interface ImageTester {

    String getErrorMessage();

    String getErrorMessageSimple();

    LanguagePack getLanguagePack();

    String getLastUriAttempted();

    boolean isIncorrectMimeType();

    boolean isOgcError();

    boolean isReadError();

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
    boolean testAnimationImage(MapLayer mapLayer);

    /**
     * Test that the passed in uri returns an image of mime-type type.
     * If an error occurred, the fail message will be stored in
     * the lastLayerTestErrorMessage variable.
     * @param uri URI of image to download
     * @param wantedType wanted MIME type of image
     * @param slowServer true if you want to allow the server to be slow
     * @return true if the test image is the correct mime type, otherwise false
     */
    boolean testImageUri(String uri, String wantedType, boolean slowServer);

    /**
     * Test a WMS server by requesting a 2x2 px image from it.
     *
     * If an error occurred, the fail message will be stored in
     * the lastLayerTestErrorMessage variable.
     * @param mapLayer mapLayer instance
     * @return true if the maplayer getmap uri poits to an image
     * otherwise false.
     */
    boolean testLayer(MapLayer mapLayer);

}
