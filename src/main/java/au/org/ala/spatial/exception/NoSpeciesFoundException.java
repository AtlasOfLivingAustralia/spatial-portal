/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.ala.spatial.exception;

/**
 * @author ajay
 */
public class NoSpeciesFoundException extends Exception {

    public NoSpeciesFoundException() {
        super("No species found");
    }

    public NoSpeciesFoundException(String msg) {
        super(msg);
    }

}
