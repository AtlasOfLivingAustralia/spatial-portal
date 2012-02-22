<%-- 
    Document   : index
    Created on : Aug 25, 2011, 10:33:50 AM
    Author     : ajay
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" %><%@
taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%><%@
taglib uri="/tld/ala.tld" prefix="ala" %>
<%@include file="common/top.jsp" %>


<div id="content">


    <header id="page-header">
        <div class="inner">
            <section id="content-search">
                <h1>Spatial Web Services</h1>
                <p>Following are a list of ALA Spatial web services.</p>
            </section>
        </div><!--inner-->
    </header>

    <div class="inner">
        <div class="col-wide last" style="width:100%">


            <h3>Layers Web Services</h3>

            <ul>
                <li>Layers<ul>
                        <li><strong>Get a list of all layers:</strong> <a href="/ws/layers">/ws/layers</a></li>
                        <li><strong>Get a list of all environmental/grided layers:</strong> <a href="/ws/layers/grids">/ws/layers/grids</a></li>
                        <li><strong>Get a list of all contextual layers:</strong> <a href="/ws/layers/shapes">/ws/layers/shapes</a></li>
                    </ul></li>

                <li>Fields<ul>
                        <li><strong>Get a list of all fields:</strong> <a href="/ws/fields">/ws/fields</a></li>
                        <li><strong>Get information about a specific field, given a field id:</strong> /ws/field/{id} e.g. <a href="/ws/field/cl22">/ws/field/cl22</a></li>
                    </ul></li>

                <li>Objects<ul>
                        <li><strong>Get a list of objects, given the field id:</strong> /ws/objects/{id} e.g. <a href="/ws/objects/cl22">/ws/objects/cl22</a></li>
                        <li><strong>Get information about an object, given its pid</strong> /ws/object/{pid} e.g. <a href="/ws/object/3742602">/ws/object/3742602</a></li>
                        <li><strong>Download a shape object as KML, given its pid:</strong> /ws/shape/kml/{pid} e.g. <a href="/ws/shape/kml/3742602">/ws/shape/kml/3742602</a></li>
                        <li><strong>Download a shape object as WKT, given its pid:</strong> /ws/shape/wkt/{pid} <a href="/ws/shape/wkt/3742602">/ws/shape/wkt/3742602</a></li>
                        <li><strong>Download a shape object as GeoJSON, given its pid:</strong> /ws/shape/geojson/{pid} <a href="/ws/shape/geojson/3742602">/ws/shape/geojson/3742602</a></li>
                        <li><strong>Get the nearest objects to a coordinate</strong> /ws/objects/{id}/{lat}/{lng}?limit=40 e.g. <a href="/ws/objects/cl915/-22.465864536394/124.419921875?limit=10">/ws/objects/cl915/-22.465864536394/124.419921875?limit=10</a></li>
                    </ul></li>

                <li>Search<ul>
                        <li><strong>Search for gazzetter localities:</strong> /search?q={free text} e.g. <a href="/ws/search?q=canberra">/ws/search?q=canberra</a></li>
                    </ul></li>

                <li>Intersect<ul>
                        <li><strong>Intersect a layer(s) at a given set of coordinates. Multiple field ids or layer names can be specified separated by a comma (e.g. cl22,cl23):</strong> /ws/intersect/{id}/{latitude}/{longitude} e.g. <a href="/ws/intersect/cl22/-29.911/132.769">/ws/intersect/cl22/-29.911/132.769</a></li>
                        <li><strong>Batch intersect a layer(s) at given coordinates. Multiple field ids or layer names can be specified separated by a comma (e.g. cl22,cl23).  Limited to 1000 coordinates.:</strong> /ws/intersect/batch e.g. <a href="/ws/intersect/batch?fids=cl22&points=-29.911,132.769,-20.911,122.769">/ws/intersect/batch?fids=cl22&amp;points=-29.911,132.769,-20.911,122.769</a></li>
                        <li><strong>Check batch intersect status with a batchId:</strong> /ws/intersect/batch/{batchId} e.g. /ws/intersect/batch/1234</li>
                        <li><strong>Download a finished batch intersect with a batchId as zipped file 'sample.csv':</strong> /ws/intersect/batch/download/{batchId} e.g. /ws/intersect/batch/download/1234</li>
                    </ul></li>

                <li>Distributions<ul>
                    <li><strong>Get a list of all distributions:</strong> <a href="/ws/distributions">/ws/distributions</a>
                        <ul>
                            <li><strong>min_depth</strong> - min depth in metres</li>
                            <li><strong>max_depth</strong> - max depth in metres</li>
                            <li><strong>wkt</strong> - well known text string to use to intersect distributions</li>
                            <li><strong>fid</strong> - the id for the alayer</li>
                            <li><strong>objectName</strong> - the name of the object in the layer used to intersect distributions</li>
                        </ul>
                    </li>
                    <li><strong>Get a list of all distributions for radius:</strong> <a href="/ws/distributions/radius">/ws/distributions/radius</a>
                        <ul>
                            <li><strong>lat</strong> - latitude</li>
                            <li><strong>lon</strong> - longitude</li>
                            <li><strong>radius</strong> - radius in metres</li>
                            <li><strong>min_depth</strong> - min depth in metres</li>
                            <li><strong>max_depth</strong> - max depth in metres</li>
                        </ul>
                    </li>
                    <li><strong>Get information about a specific distribution, given a spcode:</strong> /ws/distribution/{spcode} e.g. <a href="/ws/distribution/37031044">/ws/distribution/37031044</a> (Arafura Skate)</li>
                    <li><strong>Get information about a specific distribution, given a LSID:</strong> /ws/distribution/lsid/{lsid} e.g. <a href="/ws/distribution/lsidurn:lsid:biodiversity.org.au:afd.taxon:2386db84-1fdd-4c33-a2ea-66e13bfc8cf8">/ws/distribution/lsidurn:lsid:biodiversity.org.au:afd.taxon:2386db84-1fdd-4c33-a2ea-66e13bfc8cf8</a> (Kapala Stingaree)</li>
                </ul></li>


                <li>Tabulation<ul>
                        <li><strong>Get a list of tabulations:</strong> <a href="/ws/tabulations">/ws/tabulations</a></li>
                        <!-- <li><strong>Get a list of tabulations as HTML:</strong> <a href="/ws/tabulations/html">/ws/tabulations/html</a></li> -->
                        <li><strong>Get tabulation for a single layer as HTML:</strong> /ws/tabulation/cl22/html?wkt={valid wkt polygon geometry} e.g. <a href="/ws/tabulation/cl22/html.html?wkt=POLYGON((130%20-24,138%20-24,138%20-20,130%20-20,130%20-24))">/ws/tabulation/cl22/html.html?wkt=POLYGON((130 -24,138 -24,138 -20,130 -20,130 -24))</a></li>
                        <li><strong>Get area tabulation for 2 layers, given their id's:</strong> /ws/tabulation/area/{id}/{id} e.g. <a href="/ws/tabulation/area/cl22/cl23">/ws/tabulation/area/cl22/cl23</a></li>
                        <li><strong>Get area tabulation as CSV for 2 layers, given their id's:</strong> /ws/tabulation/area/{id}/{id}/data.csv e.g. <a href="/ws/tabulation/area/cl22/cl23/data.csv">/ws/tabulation/area/cl22/cl23/data.csv</a></li>
                        <li><strong>Get area tabulation as HTML for 2 layers, given their id's:</strong> /ws/tabulation/area/{id}/{id}/data.html e.g. <a href="/ws/tabulation/area/cl22/cl23/data.html">/ws/tabulation/area/cl22/cl23/data.html</a></li>
                        <li><strong>Get tabulation within an area as HTML for 2 layers, given their id's:</strong> /ws/tabulation/{id}/{id}/html?wkt={valid wkt polygon geometry} e.g. <a href="/ws/tabulation/cl22/cl23/html.html?wkt=POLYGON((130%20-24,138%20-24,138%20-20,130%20-20,130%20-24))">/ws/tabulation/cl22/cl23/html.html?wkt=POLYGON((130 -24,138 -24,138 -20,130 -20,130 -24))</a></li>
                    </ul></li>
            </ul>

            <h3>Occurrences</h3>
            <ul>
                <li><strong>Static Species Density Heatmap </strong><a href="http://biocache.ala.org.au/ws/density/map?q=*:*">http://biocache.ala.org.au/ws/density/map?q=*:*</a> <a href="http://biocache.ala.org.au/ws/density/map?q=Sarcophilus%20harrisii">http://biocache.ala.org.au/ws/density/map?q=Sarcophilus%20harrisii</a></li> - returns heatmap image (optional param forceRefresh=true will regenerate the image)
                <li><strong>Static Species Density Legend: </strong><a href="http://biocache.ala.org.au/ws/density/legend?q=*:*">http://biocache.ala.org.au/ws/density/legend?q=*:*</a></li> - returns associated legend image (optional param forceRefresh=true will regenerate the image)
            </ul>

            <h3>Webportal Services</h3>
            <p>These Webportal services are available at <a href="http://biocache.ala.org.au/ws">http://biocache.ala.org.au/ws</a> </p>
            <ul>
                These services will include all records that satisfy the q, fq and wkt parameters.
                <ul>
                    <li>q - the initial query</li>
                    <li>fq - filters to be applied to the original query</li>
                    <li>wkt - filter polygon area to be applied to the original query</li>
                    <li>fl - a comma separated list of fields to include (contains a list of default)</li>
                    <li>pageSize - download limit (may be overridden)</li>
                </ul>

                <li><strong>Short Query Parameters:</strong>
                    <ul>
                        <li><strong>Construction:</strong> /webportal/params <br />
                            POST service.<br />
                            Stores q and wkt parameters.<br />
                            Returns a short <b>value</b> that can be used as the initial q value in other services for webportal. e.g. q=qid:<b>value</b>
                        </li>
                        <li><strong>Test: </strong> /webportal/params/<b>value</b>
                            Test if a short query parameter is valid.<br />
                            Returns true or false</li>
                    </ul>
                </li>
                <li><strong>Occurrences Bounding Box:</strong> /webportal/bbox <br />
                    Returns CSV of bounding box of occurrences eg. <a href="http://biocache.ala.org.au/ws/webportal/bbox?q=macropus">http://biocache.ala.org.au/ws/webportal/bbox?q=macropus</a></li>
                <li><strong>Data Providers</strong> /webportal/dataProviders eg. <a href="http://biocache.ala.org.au/ws/webportal/dataProviders?q=macropus">http://biocache.ala.org.au/ws/webportal/dataProviders?q=macropus</a></li>
                <li><strong>Species List:</strong>
                    <ul>
                        <li><strong>Get species list:</strong> /webportal/species eg. <a href="http://biocache.ala.org.au/ws/webportal/species?q=macropus&pageSize=100">http://biocache.ala.org.au/ws/webportal/species?q=macropus&amp;pageSize=100</a></li>
                        <li><strong>Get species list as CSV:</strong> /webportal/species.csv eg. <a href="http://biocache.ala.org.au/ws/webportal/species.csv?q=macropus&wkt=POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))&pageSize=100">http://biocache.ala.org.au/ws/webportal/species.csv?q=macropus&amp;wkt=POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))&amp;pageSize=100</a></li>
                    </ul>
                </li>
                <li><strong>Occurrences:</strong>
                    <ul>
                        <li><strong>Get occurrences:</strong> /webportal/occurrences eg. <a href="http://biocache.ala.org.au/ws/webportal/occurrences?q=macropus">http://biocache.ala.org.au/ws/webportal/occurrences?q=macropus</a></li>
                        <li><strong>Get occurrences as gzipped CSV:</strong> /webportal/occurrences.gz eg. <a href="http://biocache.ala.org.au/ws/webportal/occurrences.gz?q=macropus&fl=longitude,latitude">http://biocache.ala.org.au/ws/webportal/occurrences.gz?q=macropus&amp;fl=longitude,latitude</a></li>
                    </ul>
                </li>
            </ul>

            <h3>Webportal WMS Service</h3>
            <p>A working example is located <a href="http://code.google.com/p/alageospatialportal/wiki/SimpleWMSExample">here.</a></p>
            <ul>
                <li><strong>Tile:</strong> /webportal/wms/reflect
                    <ul>
                        <li>BBOX - EPSG900913 bounding box. e.g. &amp;BBOX=12523443.0512,-2504688.2032,15028131.5936,0.3392000021413</li>
                        <li>WIDTH - width in pixels</li>
                        <li>HEIGHT - height in pixels</li>
                        <li>CQL_FILTER - query parameter</li>
                        <li>ENV - additional parameters. e.g. ENV=color%3Acd3844%3Bsize%3A3%3Bopacity%3A0.8
                            <ul>
                                <li>color - hex RGB values. e.g. colour:cd3844</li>
                                <li>size - radius of points in pixels</li>
                                <li>opacity - opacity value 0 - 1</li>
                                <li>sel - fq parameter applied to CQL_FILTER.  Matching occurrences will be highlighted on the map in a Red circle</li>
                                <li>uncertainty - presence of the uncertainty parameter draws uncertainty circles to a fixed maximum of 30km</li>
                                <li>colormode - facet colouring type.  <br />
                                    <table>
                                        <tr><td>colourmode</td><td>description</td></tr>
                                        <tr><td>-1</td><td>(default) use color value</td></tr>
                                        <tr><td>grid</td><td>map as density grid.  Grid cells drawn are not restricted to within any query WKT parameters.</td></tr>
                                        <tr><td>facetname</td><td>colour as categories in a facet</td></tr>
                                        <tr><td>facetname,cutpoints</td><td>colour as range in a facet using the supplied
                                                comma separated cutpoints.  4 to 10 values are required.  Include minimum and maximum.
                                                Minimum and maximum values do not need to be accurate.
                                                e.g. colormode:year,1800,1900,1950,1970,1990,2010</td></tr>
                                    </table>
                                </li>
                            </ul>
                        </li>
                    </ul>
                <li><strong>Legend:</strong> /webportal/legend <br />
                    Get a CSV legend.<br />
                    Parameters:
                    <ul>
                        <li>q - CQL_FILTER value</li>
                        <li>cm - ENV colormode value</li>
                    </ul>
                    Contains columns:
                    <ul>
                        <li>name - legend item name</li>
                        <li>red - 0-255</li>
                        <li>green - 0-255</li>
                        <li>blue - 0-255</li>
                        <li>count - number of occurrences for this legend category in the q parameter</li>
                    </ul>
                </li>
            </ul>

            <h3>Analysis Web Services</h3>
            <p>There are three stages in using analysis web services; Start analysis, Monitor analysis, Retrieve output.</p>
            <ul>
                <li><strong>MaxEnt Prediction</strong>
                    <p>Start Maxent /ws/maxent/procesgeoq, POST request.  E.g. http://spatial.ala.org.au/alaspatial/ws/maxent/processgeoq</p>
                    Parameters:
                    <ul>
                        <li>taxonid - this will be the name of maxent model.  E.g. “Macropus Rufus”.</li>
                        <li>taxonlsid – Life Science Identifier that is required but not currently used.  E.g. “urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537”.</li>
                        <li>species – A csv file with a header record and containing all species points.  Column order is species name, longitude (decimal degrees), latitude (decimal degrees). E.g.
                            “Species,longitude,latitude
                            Macropus Rufus,122,-20
                            Macropus Rufus,123,-18”.
                        </li>
                        <li>area - bounding area in Well Known Text (WKT) format.  E.g.  “POLYGON((118 -30,146 -30,146 -11,118 -11,118 -30))”.</li>
                        <li>envlist – a list of environmental and contextual layers as colon separated short names.  E.g. “bioclim_bio1:bioclim_bio12:bioclim_bio2:landuse”.
                            <ul>
                                <li>List of analysis valid environmental layer short names <a href="http://spatial.ala.org.au/alaspatial/ws/spatial/settings/layers/environmental/string">here</a>. These are a subset of all layers <a href="http://spatial.ala.org.au/layers.">here</a></li>
                                <li>List of analysis valid contextual layers; landcover, landuse, vast, native_veg, present_veg </li>
                            </ul>
                        </li>
                        <li>txtTestPercentage - optional percentage of records dedicated to testing.  E.g. “23”.</li>
                        <li>chkJackKnife - optional parameter to enable/disable Jacknifing.  E.g. “Y”.</li>
                        <li>chkResponseCurves – optional parameter to enable/disable plots of response curves.  E.g. “Y”.</li>
                    </ul>
                    <br />
                    <p>Returns: analysis id.  E.g. “123”.</p>
                </li>
                <li><strong>Classification (ALOC)</strong>
                    <p>Start ALOC /ws/aloc/processgeoq, POST request.  E.g. http://spatial.ala.org.au/alaspatial/ws/aloc/processgeoq</p>
                    Parameters:
                    <ul>
                        <li>gc - number of groups to try and produce. No guarantee that convergence to the exact number will occur. If not, it will generate as close a number of groups as possible.  E.g. “20”.</li>
                        <li>area - bounding area in Well Known Text (WKT) format.  E.g.  “POLYGON((118 -30,146 -30,146 -11,118 -11,118 -30))”.</li>
                        <li>envlist – a list of environmental layers as colon separated short names.  E.g. “bioclim_bio1:bioclim_bio12:bioclim_bio2”.
                            <ul>
                                <li>List of analysis valid environmental layer short names <a href="http://spatial.ala.org.au/alaspatial/ws/spatial/settings/layers/environmental/string">here</a>. These are a subset of all layers <a href="http://spatial.ala.org.au/layers.">here</a></li>
                                <li>List of analysis valid contextual layers; landcover, landuse, vast, native_veg, present_veg </li>
                            </ul>
                        </li>
                    </ul>
                    <br />
                    <p>Returns: analysis id.  E.g. “123”.</p>
                </li>
                <li><strong>Sites by Species, Occurrence density, Species richness</strong>
                    <p>Start with /ws/sitesbyspecies/processgeoq, POST request.  E.g. http://spatial.ala.org.au/alaspatial/ws/sitesbyspecies/processgeoq</p>
                    Parameters, must include at least one of the optional parameters:
                    <ul>
                        <li>speciesq - Data name that appears in the output.  E.g. “genus:Macropus”.</li>
                        <li>qname - Data name that appears in the output.  E.g. “Macropus”.</li>
                        <li>area - Bounding area in Well Known Text (WKT) format.  E.g.  “POLYGON((118 -30,146 -30,146 -11,118 -11,118 -30))”.</li>
                        <li>gridsize - Size of output grid cells in decimal degrees.  E.g. "0.1"</li>
                        <li>movingaveragesize - Size of output moving average window for occurrence density and species density layers.  E.g. for a 9x9 grid cell window use "&amp;movingaveragesize=9"</li>
                        <li>sitesbyspecies - (optional) Include this parameter to produce a sites by species list.  E.g. "&amp;sitesbyspecies=1"</li>
                        <li>occurrencedensity - (optional) Include this parameter to produce an occurrence density layer.  E.g. "&amp;occurrencedensity=1"</li>
                        <li>speciesdensity - (optional) Include this parameter to produce a species richness layer.  E.g. "&amp;speciesdensity=1"</li>

                    </ul>
                    <br />
                    <p>Returns: analysis id.  E.g. “123”.</p>
                </li>
                <li><strong>Monitor Analysis</strong>
                    <ul>
                        <li>/ws/jobs/state?pid=&lt;analysis id&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/state?pid=123
                            <br />returns one of "WAITING", "RUNNING", "SUCCESSFUL", "FAILED", "CANCELLED", </li>
                        <li>/ws/jobs/message?pid=&lt;analysis id>&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/message?pid=123
                            <br />returns any associated message or "job does not exist".</li>
                        <li>/ws/jobs/status?pid=&lt;analysis id&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/status?pid=123
                            <br />returns status text that may contain an estimate of time remaining or "job does not exist".</li>
                        <li>/ws/jobs/progress?pid=&lt;analysis id&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/progress?pid=123
                            <br />returns analysis job progress as a number between 0 and 1 or "job does not exist".</li>
                        <li>/ws/jobs/log?pid=&lt;analysis id&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/log?pid=123
                            <br />returns analysis job log or "job does not exist".</li>
                        <li>/ws/jobs/cancel?pid=&lt;analysis id&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/cancel?pid=123
                            <br />returns nothing if successful or "job does not exist"</li>
                    </ul>
                </li>
                <br />
                <li><strong>Retrieving Results</strong>
                    <ul>
                        <li>/ws/download/&lt;analysis id&gt;.  E.g. .  E.g. http://spatial.ala.org.au/alaspatial/ws/download/123
                            <br />downloads the zipped output of "SUCCESSFUL" analysis.</li>
                        <li>ALOC WMS service for the layer is /geoserver/wms?service=WMS&amp;version=1.1.0&amp;request=GetMap&amp;layers=ALA:aloc_&lt;analysis id&gt;&amp;styles=alastyles&amp;FORMAT=image%2Fpng.
                            <br /> E.g. http://spatial.ala.org.au/geoserver/wms/reflect?layers=ALA:aloc_123&amp;height=200&amp;width=200
                        <li>Maxent WMS service for the layer is /geoserver/wms?service=WMS&amp;version=1.1.0&amp;request=GetMap&amp;layers=ALA:species_&lt;analysis id&gt;&amp;styles=alastyles&amp;FORMAT=image%2Fpng.
                            <br />E.g. http://spatial.ala.org.au/geoserver/wms/reflect?layers=ALA:species_123&amp;height=200&amp;width=200
                        </li>
                        <li>Occurrence Density WMS service for the layer is /geoserver/wms?service=WMS&amp;version=1.1.0&amp;request=GetMap&amp;layers=ALA:odensity_&lt;analysis id&gt;&amp;styles=alastyles&amp;FORMAT=image%2Fpng.
                            <br />E.g. http://spatial.ala.org.au/geoserver/wms/reflect?layers=ALA:odensity_123&amp;height=200&amp;width=200
                        </li>
                        <li>Species Richness WMS service for the layer is /geoserver/wms?service=WMS&amp;version=1.1.0&amp;request=GetMap&amp;layers=ALA:srichness_&lt;analysis id&gt;&amp;styles=alastyles&amp;FORMAT=image%2Fpng.
                            <br />E.g. http://spatial.ala.org.au/geoserver/wms/reflect?layers=ALA:srichness_123&amp;height=200&amp;width=200
                        </li>
                    </ul>
                </li>
            </ul>

        </div>
    </div><!--inner-->


</div> <!-- content -->

<%@include file="common/bottom.jsp" %>
