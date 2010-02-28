package org.ala.spatial.exception;

/**
 *
 * @author ajayr
 */
public class MyException extends Exception {

    public MyException(Throwable cause) {
        super(cause);
    }

    public MyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyException(String message) {
        super(message);
    }

    public MyException() {
    }

}
