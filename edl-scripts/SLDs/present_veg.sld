<?xml version="1.0" encoding="UTF-8"?>
<sld:UserStyle xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml">
  <sld:Name>raster</sld:Name>
  <sld:Title>Class attribute based style</sld:Title>
  <sld:Abstract>Class attributes based style</sld:Abstract>
  <sld:FeatureTypeStyle>
    <sld:Name>name</sld:Name>
    <sld:Rule>
      <sld:Name>Other forests and woodlands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Other forests and woodlands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#009999</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Acacia shrublands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Acacia shrublands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#99FF66</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Chenopod shrublands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Chenopod shrublands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FFFF66</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name> samphire shrubs and forblands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal> samphire shrubs and forblands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FFFF00</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Eucalyptus tall open forest</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Eucalyptus tall open forest</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FF9900</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Eucalyptus open forest</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Eucalyptus open forest</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FF6600</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Casuarina forests and woodlands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Casuarina forests and woodlands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FF6666</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Melaleuca forests and woodlands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Melaleuca forests and woodlands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FF3300</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Other grasslands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Other grasslands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#CC33FF</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name> herblands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal> herblands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FF33FF</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name> sedgelands and rushlands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal> sedgelands and rushlands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#CCFF00</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Eucalyptus open woodlands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Eucalyptus open woodlands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#CCCC33</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Unknown/no data</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Unknown/no data</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#990066</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Eucalyptus low open forest</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Eucalyptus low open forest</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#99FFCC</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Mallee woodlands and shrublands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Mallee woodlands and shrublands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FFFF99</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Low closed forest and tall closed shrubland</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Low closed forest and tall closed shrubland</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#669900</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Eucalyptus woodlands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Eucalyptus woodlands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#66FFFF</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    <sld:Rule>
      <sld:Name>Heathlands</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>class</ogc:PropertyName>
          <ogc:Literal>Heathlands</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#6600FF</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
