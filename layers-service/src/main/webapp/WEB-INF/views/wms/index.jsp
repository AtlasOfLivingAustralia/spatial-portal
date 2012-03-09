<%@page contentType="text/xml" pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8"?>
<%
    String lsid = request.getParameter("lsid");
    if (lsid == null || lsid.equals("")) {
        lsid = request.getParameter("LSID");
        if (lsid == null || lsid.equals("")) {
            lsid = "lsid:urn:lsid:biodiversity.org.au:afd.taxon:558a729a-789b-4b00-a685-8843dc447319";
        }
    }

    if (!lsid.startsWith("lsid")) {
        lsid = "lsid:"+lsid;
    }
%>
<!DOCTYPE WMT_MS_Capabilities SYSTEM "http://suite.opengeo.org/geoserver/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd">
<WMT_MS_Capabilities version="1.1.1" updateSequence="0">
    <Service>
        <Name>WMS</Name>
        <Title>ALA species occurrences for <%= lsid %></Title>
        <Abstract>A simple WMS service to provide access to the Atlas of Living Australia's species occurrence records</Abstract>
        <Keywords>ALA species occurrences</Keywords>
        <OnlineResource>http://www.ala.org.au/</OnlineResource>
        <Fees>none</Fees>
        <AccessConstraints>none</AccessConstraints>
    </Service>
    <Capability>
        <Request>
            <GetCapabilities>
                <Format>text/xml</Format>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple" xlink:href="http://spatial.ala.org.au/demo/wms.jsp?lsid=<%= lsid %>"/>
                        </Get>
                        <Post>
                            <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple" xlink:href="http://spatial.ala.org.au/demo/wms.jsp?lsid=<%= lsid %>"/>
                        </Post>
                    </HTTP>
                </DCPType>
            </GetCapabilities>
            <GetMap>
                <Format>image/png</Format>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple" xlink:href="http://biocache-test.ala.org.au/ws/webportal/wms/reflect?ENV=color%3Aff0000%3Bname%3Acircle%3Bsize%3A3%3Bopacity%3A0.8&amp;CQL_FILTER=<%= lsid %>&amp;" />
                        </Get>
                    </HTTP>
                </DCPType>
            </GetMap>
            <GetFeatureInfo>
                <Format>text/plain</Format>
                <Format>application/json</Format>
                <Format>text/html</Format>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple" xlink:href="http://biocache-test.ala.org.au/ws/webportal/wms/reflect?ENV=color%3Aff0000%3Bname%3Acircle%3Bsize%3A3%3Bopacity%3A0.8&amp;CQL_FILTER=<%= lsid %>&amp;"/>
                        </Get>
                        <Post>
                            <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple" xlink:href="http://biocache-test.ala.org.au/ws/webportal/wms/reflect?ENV=color%3Aff0000%3Bname%3Acircle%3Bsize%3A3%3Bopacity%3A0.8&amp;CQL_FILTER=<%= lsid %>&amp;"/>
                        </Post>
                    </HTTP>
                </DCPType>
            </GetFeatureInfo>
        </Request>
        <Exception>
            <Format>application/vnd.ogc.se_xml</Format>
            <Format>application/vnd.ogc.se_inimage</Format>
        </Exception>
        <Layer>
            <Title>ALA Web Map Service</Title>
            <Abstract>ALA WMS occurrence service</Abstract>
            <SRS>EPSG:900913</SRS>
            <LatLonBoundingBox minx="-11151299" miny="-8937356" maxx="20157307" maxy="14544098" />
            <Layer queryable="1">
                <Name>ALA:occurrences</Name>
                <Title>ALA:occurrences</Title>
                <Abstract>ALA:occurrences. make sure the CQL_FILTER is populated with a species name</Abstract>
                <SRS>EPSG:900913</SRS>
                <LatLonBoundingBox minx="-11151299" miny="-8937356" maxx="20157307" maxy="14544098" />
            </Layer>
        </Layer>
    </Capability>
</WMT_MS_Capabilities>