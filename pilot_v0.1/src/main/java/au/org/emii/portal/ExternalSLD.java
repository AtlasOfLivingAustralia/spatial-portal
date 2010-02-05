package au.org.emii.portal;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Servlet implementation class ExternalSLD
 */
public class ExternalSLD extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private static final String EXTERNAL_SLD_DIR = "EXTERNAL_SLD_DIR";
    protected Logger logger = Logger.getLogger(this.getClass());
    /**
     * Returns a SLD file with placeholder variables substituted with supplied parameters to this servlet.
     * Requires the valid URL of an external SLD (Styled Layer Descriptor) as the parameter 'sld' or returns 'http error500'.
     * Any other supplied parameters are uppercased, searched for in the SLD and substituted.
     * EG: ?platform=1234 will cause '[*PLATFORM*]' to be substituted with '1234'
     * @author Philip
     * @see HttpServlet#HttpServlet()
     * 
     */
    public ExternalSLD() {
        super();
        // TODO Auto-generated constructor stub
        
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String s = "";
		String sld = request.getParameter("sld"); //required sld filename
		response.setContentType("text/xml");
		// hard coded for argo tracks
		// fix later for other layers if needed
		if (Validate.empty(sld) ) {
			logger.debug("missing filename for external SLD");
			response.sendError(500);
		}
		else {		
			String dir= (String) System.getProperty(EXTERNAL_SLD_DIR);	
			try {
				FileInputStream fis = new FileInputStream(dir + "/" + sld);					
				s = IOUtils.toString(fis);				
				Map x = request.getParameterMap();	
				
				// substitute all parameters
				for (Object obj  : x.keySet()) {				
					//  apart from the SLD filename
					if (obj.toString() != "sld") {
						String paramAsUpperCase = obj.toString().toUpperCase();
						s = s.replace("[*" + paramAsUpperCase + "*]", request.getParameter(obj.toString()));
						logger.debug(paramAsUpperCase + ": " + request.getParameter(obj.toString()));
					}									
				}
			}
			catch (IOException e) {
				logger.error("The file " + dir + "/" + sld + " wasnt found");	
				response.sendError(500);
			}
			
			
			
			/*String x = request.getParameter("argoid");			
			s = IOUtils.toString(fis);	
			s = s.replace("[*PLATFORM*]", x);
			*/
		}
		response.getWriter().print(s);
	}

	/**
	 * Not handled
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.sendError(500);
		response.getWriter();
	}

}
