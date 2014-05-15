package au.org.emii.portal.servlet;

import org.apache.log4j.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet implementation class ErrorHandlerServlet
 */
public class ErrorHandlerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_PAGE = "/WEB-INF/jsp/Error.jsp";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ErrorHandlerServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // get the error fields from the request - these should all be null if we've
        // already handled the error elsewhere
        Object statusCode = request.getAttribute("javax.servlet.error.status_code");
        Object message = request.getAttribute("javax.servlet.error.message");
        //Object errorType = request.getAttribute("javax.servlet.error.exception_type");
        Throwable exception = (Throwable) request.getAttribute("javax.servlet.error.exception");
        Object request_uri = request.getAttribute("javax.servlet.error.request_uri");

        // Exceptions are normally raised and logged during application or session init
        // if something has slipped through the net, log it, otherwise just display the
        // jsp error page
        Logger logger = Logger.getLogger(this.getClass());
        logger.fatal(
                "UNHANDLED EXCEPTION: HTTP ERROR " + statusCode + " Message: " + message
                        + " URI: " + request_uri + " REASON "
                        + exception.getMessage());

        // now we just display a static-ish JSP to the user - we don't use a
        // ZK page because it could well be problems with ZK that have sent
        // us here in the first place which would leave us unable to display
        // the error message
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(ERROR_PAGE);
        dispatcher.forward(request, response);

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     * response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
