<?xml version="1.0" encoding="UTF-8"?>
<sld:UserStyle xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml">
  <sld:Name>raster</sld:Name>
  <sld:Title>Class attribute based style</sld:Title>
  <sld:Abstract>Class attributes based style</sld:Abstract>
  <sld:FeatureTypeStyle>
    <sld:Name>name</sld:Name>
    <sld:Rule>
      <sld:Name>present</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>present</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#009999</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>absent</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>absent</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#99FF66</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>unknown</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>unknown</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FFFF66</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
