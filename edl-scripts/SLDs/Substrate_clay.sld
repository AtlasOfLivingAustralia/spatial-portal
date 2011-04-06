<?xml version="1.0" encoding="UTF-8"?><sld:UserStyle xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml">
  <sld:Name>raster</sld:Name>
  <sld:Title>A very simple color map</sld:Title>
  <sld:Abstract>A very basic color map</sld:Abstract>
  <sld:FeatureTypeStyle>
    <sld:Name>name</sld:Name>
    <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
    <sld:Rule>
      <sld:RasterSymbolizer>
        <sld:Geometry>
          <ogc:PropertyName>geom</ogc:PropertyName>
        </sld:Geometry>
        <sld:ChannelSelection>
          <sld:GrayChannel>
            <sld:SourceChannelName>1</sld:SourceChannelName>
          </sld:GrayChannel>
        </sld:ChannelSelection>
        <sld:ColorMap>
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="-999"/>
          <sld:ColorMapEntry color="#002DD0" quantity="3.1886792" label="3.1886792 %"/>
          <sld:ColorMapEntry color="#005BA2" quantity="6.1640625"/>
          <sld:ColorMapEntry color="#008C73" quantity="10.374384"/>
          <sld:ColorMapEntry color="#00B944" quantity="16.961311"/>
          <sld:ColorMapEntry color="#00E716" quantity="24.690044"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="29.67"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="32.152172"/>
          <sld:ColorMapEntry color="#FFC814" quantity="38.06784"/>
          <sld:ColorMapEntry color="#FFA000" quantity="44.43617"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="54.347828"/>
          <sld:ColorMapEntry color="#FF0000" quantity="68.82353" label="68.82353 %"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>