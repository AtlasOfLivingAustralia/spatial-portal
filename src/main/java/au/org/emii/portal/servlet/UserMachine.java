package au.org.emii.portal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
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
