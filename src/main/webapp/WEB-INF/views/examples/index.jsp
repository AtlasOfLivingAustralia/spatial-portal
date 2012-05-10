<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@taglib uri="/tld/ala.tld" prefix="ala" %>
<%@include file="../common/top.jsp" %>
<header id="page-header">
    <div class="inner">
        <nav id="breadcrumb"><ol><li><a href="http://www.ala.org.au">Home</a></li> <li><a href="http://spatial.ala.org.au">Mapping &#038; analysis</a></li> <li><a href="http://spatial.ala.org.au/ws/">Spatial Web Services</a></li><li  class="last">ALA Spatial demos</li></ol></nav>
        <section id="content-search">
            <h1>ALA Spatial Demos</h1>
            <p>Following are a list of demos available for the ALA Spatial web services.</p>
        </section>
    </div><!--inner-->

</header>
<div class="inner">

    <div class="col-wide last" style="width:100%">
        <section class="results">
            <table id="layerstable" class="table-borders" style="width:100%">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Description</th>
                        <th>Components used</th>
                    </tr>
                    <tr>
                        <td><a href="/ws/examples/specieswms">Species occurrence WMS</a></td>
                        <td>
                            This example shows how to use the species occurrences
                            available via ALA's web services using OpenLayers.
                        </td>
                        <td>
                            Species WMS, Species autocomplete 
                        </td>
                    </tr>
                    <tr>
                        <td><a href="/ws/examples/layerswms">Spatial layer WMS</a></td>
                        <td>
                            This example shows how to use the environmental and
                            contextual layers available via ALA's web services using OpenLayers.
                        </td>
                        <td>
                            Layers WMS, Layers autocomplete
                        </td>
                    </tr>
                    <tr>
                        <td><a href="/ws/examples/specieswmsg">Species and Layer WMS using Google</a></td>
                        <td>
                            This example shows how to use the species, environmental and
                            contextual layers available via ALA's web services using Google API.
                        </td>
                        <td>
                            Species WMS, Species autocomplete Layers WMS, Layers autocomplete
                        </td>
                    </tr>
                </thead>
                <tbody>
                </tbody>
            </table>
        </section>

    </div><!--col-wide-->

</div><!--inner-->


<%@include file="../common/bottom.jsp" %>