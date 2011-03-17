<?xml version="1.0" encoding="UTF-8"?>
<sld:UserStyle xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml">
  <sld:Name>raster</sld:Name>
  <sld:Title>Class attribute based style</sld:Title>
  <sld:Abstract>Class attributes based style</sld:Abstract>
  <sld:FeatureTypeStyle>
    <sld:Name>name</sld:Name>
    <sld:Rule>
      <sld:Name>No practices reported</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>No practices reported</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#009999</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>No cultivation (apart from actual sowing operation)</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>No cultivation (apart from actual sowing operation)</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#99FF66</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Equal (2) and (3)</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Equal (2) and (3)</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FFFF66</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Other cultivation</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Other cultivation</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FFFF00</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Equal (1) and (3)</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Equal (1) and (3)</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FF9900</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>One or two cultivations only (immediately prior to sowing)</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>One or two cultivations only (immediately prior to sowing)</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FF6600</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>No cereal producers</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>No cereal producers</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FF6666</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
