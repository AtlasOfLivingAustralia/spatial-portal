/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 *
 * @author brendon
 */
public class DepthServlet extends HttpServlet {

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/xml");
        PrintWriter out = response.getWriter();
        String sLon = request.getParameter("lon");
        String sLat = request.getParameter("lat");
        try {


            String sDepth = fetchDepth(sLon, sLat);



            out.println("<?xml version=\"1.0\"?>");
            out.println("<root>");
            out.println("<latitude>");
            out.println(sLat);
            out.println("</latitude>");
            out.println("<longitude>");
            out.println(sLon);
            out.println("</longitude>");
            out.println("<depth>");
            out.println(sDepth);
            out.println("</depth>");

            out.println("</root>");


        } finally {
            out.close();
        }
    }

    private String fetchDepth(String sLon, String sLat) throws ServletException {

        String sDepth = "null";
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        String sQuery = null;

       

        try {
            // Begin unit of work
            
            session.beginTransaction();
            sQuery = "SELECT depth FROM bathy.world_depth WHERE ST_DWithin(geom, GeomFromText('POINT(" + sLon + " " + sLat +  ")', 4326), 0.1) ORDER BY distance(geom, GeomFromText('POINT(" + sLon + " " + sLat +  ")', 4326)) LIMIT 1;";

            SQLQuery qryDepth = session.createSQLQuery(sQuery);
            sDepth = String.valueOf(qryDepth.uniqueResult());

      

            session.close();



            // End unit of work
            
        }
        catch (Exception ex) {
           
        }



        return sDepth;


    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
