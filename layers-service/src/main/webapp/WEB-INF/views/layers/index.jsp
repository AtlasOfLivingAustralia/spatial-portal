<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@taglib uri="/tld/ala.tld" prefix="ala" %>
<%@include file="../common/top.jsp" %>
<header id="page-header">
    <div class="inner">
        <nav id="breadcrumb"><ol><li><a href="http://www.ala.org.au">Home</a></li> <li><a href="http://spatial.ala.org.au">Mapping &#038; analysis</a></li> <li class="last">Spatial Web Services</li></ol></nav>
        <section id="content-search">
            <h1>Spatial layers</h1>
            <p>Following are a list of ALA Spatial web services.</p>
        </section>
    </div><!--inner-->

</header>
<div class="inner">

    <div class="col-narrow" style="display:none">
        <h2>Refine Results</h2>

        <div id="currentFilterHolder" class="currentfacets">
            <h3>Current Filters</h3>
            <ul>
                <!--
                <li>Resource type: <strong>Website</strong> <a href="#" onclick="removeFilter('resourceType','website',this);return false;" class="button small negative removeLink" title="remove filter">X</a></li>

                <li>Integration status: <strong>Data available</strong> <a href="#" onclick="removeFilter('resourceType','website',this);return false;" class="button small negative removeLink" title="remove filter">X</a></li>
                -->
            </ul>
        </div>

        <ul class="facets">
            <li>License type
                <ul>
                    <li><a href="">Custom license</a> (192)</li>

                    <li><a href="">No information</a> (86)</li>
                    <li><a href="">CC BY-NC</a> (27)</li>
                    <li><a href="">CC BY</a> (17)</li>
                    <li><a href="">CC BY-NC-SA</a> (6)</li>

                    <li><a href="">CC BY-SA</a> (6)</li>
                </ul>
            </li>
            <li>License version
                <ul>
                    <li><a href="">No information</a> (278)</li>

                    <li><a href="">CC 3.0</a> (41)</li>
                    <li><a href="">CC 2.5</a> (15)</li>
                </ul>
            </li>
            <li>Content type
                <ul>

                    <li><a href="">Scientific names</a> (102)</li>
                    <li><a href="">Images</a> (99)</li>
                    <li><a href="">Common names</a> (87)</li>
                    <li><a href="">Point occurrence data</a> (72)</li>

                    <li><a href="">Description</a> (64)</li>
                    <li><a class="button">+ show more</a></li>
                </ul>
            </li>
            <li>Institution
                <ul>
                    <li><a href="">No information</a> (304)</li>

                    <li><a href="">CSIRO</a> (8)</li>
                    <li><a href="">Australian Museum</a> (4)</li>
                    <li><a href="">DSEWPaC</a> (3)</li>
                    <li><a href="">James Cook University</a> (2)</li>

                    <li><a class="button">+ show more</a></li>
                </ul>
            </li>
        </ul>
    </div>

    <div class="col-wide last" style="width:100%">
        <section id="results-options">
            <span class="alignleft">

                <span id="resultsReturned"><strong>${fn:length(layers)}</strong> layers found.</span>
                <!--
                <form action="index">
                    <input type="text" name="dr-search" id="dr-search" placeholder="Search within results" />
                    <a class="button" title="Search within results" id="dr-search-link">Search</a>
                </form>
                -->
            </span>
            <span class="alignright">
                <!--<a class="button caution" href="javascript:reset()" title="Remove all filters and sorting options">Reset list</a>-->
                <a href="#" id="downloadCSVLink" class="button" title="Download metadata for datasets as a CSV file">Download as CSV</a>
                <a href="#" id="downloadJSONLink" class="button" title="Download metadata for datasets as a JSON file">Download as JSON</a>

            </span>
        </section>

        <section class="results">
            <c:choose>
                <c:when test="${fn:length(layers) > 0}">
                    <table id="layerstable" class="table-borders" style="width:100%">
                        <thead>
                            <tr>
                                <th>Classification 1</th>
                                <th>Classification 2</th>
                                <th>Display name</th>
                                <th>Short name</th>
                                <th>Description</th>
                                <th>Type</th>
                                <th>Metadata contact organization</th>
                                <th>Keywords</th>
                                <th>Preview</th>
                                <!-- <th>Reference date</th> -->
                                <!--<th>Actions</th>-->
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${layers}" var="layer" varStatus="status">
                                <tr>
                                    <td>${layer.classification1}</td>
                                    <td>${layer.classification2}</td>
                                    <td><a href="/layers/more/${layer.name}">${layer.displayname}</a></td>
                                    <td style="max-width:80px" class="wrapword">${layer.name}</td>
                                    <td>${layer.description}</td>
                                    <c:choose>
                                        <c:when test="${layer.type eq 'Environmental'}">
                                            <td>Environmental (gridded) ${layer.scale}</td>
                                        </c:when>
                                        <c:when test="${layer.type eq 'Contextual'}">
                                            <td>Contextual (polygonal) ${layer.scale}</td>
                                        </c:when>
                                        <c:otherwise>
                                            <td>${layer.type} ${layer.scale}</td>
                                        </c:otherwise>
                                    </c:choose>
                                    <td>${layer.source}</td>
                                    <td>${layer.keywords}</td>
                                    <td>
                                        <!-- <img src="http://spatial-dev.ala.org.au/output/layerthumbs/ALA:${layer.name}.jpeg" width="200px" /> -->
                                        <!-- <img src="/geoserver/wms/reflect?layers=ALA:${layer.name}&width=200&height=200" /> -->
                                        <br />
                                        <!--<a href="/geoserver/wms/kml?layers=ALA:${layer.name}">KML</a>-->
                                    </td>
                                    <!-- <td>${layer.citation_date}</td> -->
                                    <!--
                                    <td>
                                        <a href="layers/edit/${layer.id}">edit</a>
                                    </td>
                                    -->
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:when>
                <c:otherwise>
                    <ul><li>No layers available</li></ul>
                </c:otherwise>
            </c:choose>
        </section>

        <!--
        <nav class="pagination">
            <ol>
                <li id="prevPage">&laquo Previous</li>

                <li class="currentPage">1</li>
                <li><a href="javascript:gotoPage(2);">2</a></li>
                <li><a href="javascript:gotoPage(3);">3</a></li>
                <li><a href="javascript:gotoPage(4);">4</a></li>
                <li><a href="javascript:gotoPage(5);">5</a></li>
                <li><a href="javascript:gotoPage(6);">6</a></li>

                <li><a href="javascript:gotoPage(7);">7</a></li>
                <li><a href="javascript:gotoPage(8);">8</a></li>
                <li><a href="javascript:gotoPage(9);">9</a></li>
                <li id="nextPage"><a href="javascript:nextPage();">Next &raquo</a></li>
            </ol>
        </nav>
        -->
    </div><!--col-wide-->

</div><!--inner-->


<script type="text/javascript" src="/actions/static/scripts/jquery.dataTables.min.js"></script>
<script type="text/javascript">

    $(document).ready(function() {

        // setup the table
        $('#layerstable').dataTable( {
            "aaSorting": [[ 2, "asc" ]],
            "aLengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]], 
            "sPaginationType": "full_numbers",
            "sDom": '<"sort-options"fl<"clear">>rt<"pagination"ip<"clear">>',
            "oLanguage": {
                "sSearch": ""
            }
        } );

        $("div.dataTables_filter input").attr("placeholder", "Filter within results");

        $('#downloadCSVLink').click(function() {
            downloadLayers("csv");
        });
        $('#downloadJSONLink').click(function() {
            downloadLayers("json");
        });
    });

    function downloadLayers(type) {
        var downloadurl = "/layers-service/layers";
        var query = $("div.dataTables_filter input").val();
        if (type == "json") {
            downloadurl += ".json";
        } else {
            downloadurl += ".csv";
        }
        if (query != "") {
            downloadurl += "?q="+query;
        }
        location.href = downloadurl; 
    }
</script>




<%@include file="../common/bottom.jsp" %>