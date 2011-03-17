<?xml version="1.0" encoding="UTF-8"?>
<sld:UserStyle xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml">
  <sld:Name>raster</sld:Name>
  <sld:Title>Class attribute based style</sld:Title>
  <sld:Abstract>Class attributes based style</sld:Abstract>
  <sld:FeatureTypeStyle>
    <sld:Name>name</sld:Name>
    <sld:Rule>
      <sld:Name>Strict nature reserve</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>iucn</ogc:PropertyName>
          <ogc:Literal>IA</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <TextSymbolizer>
		<Label>
			<ogc:PropertyName>name</ogc:PropertyName>
		</Label>
	
		<Font>
			<CssParameter name="font-family">Times New Roman</CssParameter>
			<CssParameter name="font-style">Normal</CssParameter>
			<CssParameter name="font-size">16</CssParameter>
			<CssParameter name="font-weight">bold</CssParameter>
		</Font>
		
		<Stroke>
			<CssParameter name="stroke">#FFFFFF</CssParameter>
              		<CssParameter name="stroke-width">1</CssParameter>
		</Stroke>
		<Fill>
			<CssParameter name="fill">#000000</CssParameter>
		</Fill>
	</TextSymbolizer>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#d20000</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
     <sld:Rule>
      <sld:Name>Wilderness area</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>iucn</ogc:PropertyName>
          <ogc:Literal>IB</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FF0000</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
     <sld:Rule>
      <sld:Name>National Park</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>iucn</ogc:PropertyName>
          <ogc:Literal>II</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#b33134</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
     <sld:Rule>
      <sld:Name>Natural monument or feature</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>iucn</ogc:PropertyName>
          <ogc:Literal>III</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#ea7400</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
     <sld:Rule>
      <sld:Name>Habitat/species management area</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>iucn</ogc:PropertyName>
          <ogc:Literal>IV</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#FFFF00</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
     <sld:Rule>
      <sld:Name>Protected landscape/seascape</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>iucn</ogc:PropertyName>
          <ogc:Literal>V</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#009bbc</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
     <sld:Rule>
      <sld:Name>Protected area with sustainable use of natural resources</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>iucn</ogc:PropertyName>
          <ogc:Literal>VI</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#009999</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
     <sld:Rule>
      <sld:Name>non-IUCN protected area</sld:Name>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:PropertyName>iucn</ogc:PropertyName>
          <ogc:Literal>NA</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <sld:PolygonSymbolizer>
        <sld:Fill>
          <sld:CssParameter name="fill">#00ad52</sld:CssParameter>
        </sld:Fill>
      </sld:PolygonSymbolizer>
    </sld:Rule>
    
  </sld:FeatureTypeStyle>
</sld:UserStyle>