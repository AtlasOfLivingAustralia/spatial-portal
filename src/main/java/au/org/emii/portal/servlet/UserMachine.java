package au.org.emii.portal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.HttpRequestHandler;

/**
 *  A servlet that gets the user information such as IP and hostname
 *  This is a hack as ZK seems to be a bit crap when deployed on the server
 *
 * @author ajay
 */
public class UserMachine implements HttpRequestHandler {
   

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        /*
        out.write("\nip: " + request.getRemoteAddr());
        out.write("\nhost: " + request.getRemoteHost());
        out.write("\nheaders: ");
        Enumeration it2 = request.getHeaderNames();
        while (it2.hasMoreElements()) {
            String name = (String)it2.nextElement();
            out.println("\n" + name + ": " + request.getHeader(name));
        }
        //request.getSession(false).getId();
         *
         */

        String ip = "";
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null) {
            if (xff.trim().equalsIgnoreCase("")) {
                ip = request.getRemoteAddr();
            } else {
                ip = xff; 
            }
        }

        out.write(ip);
        out.close(); 
    }

}
