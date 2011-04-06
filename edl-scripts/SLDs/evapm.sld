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
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="-9999"/>
          <sld:ColorMapEntry color="#002DD0" quantity="46.419754" label="46.419754  (ml/5km x 5km pixel)"/>
          <sld:ColorMapEntry color="#005BA2" quantity="139.46463"/>
          <sld:ColorMapEntry color="#008C73" quantity="166.75769"/>
          <sld:ColorMapEntry color="#00B944" quantity="189.4032"/>
          <sld:ColorMapEntry color="#00E716" quantity="211.86038"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="230.2955"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="243.99095"/>
          <sld:ColorMapEntry color="#FFC814" quantity="255.56367"/>
          <sld:ColorMapEntry color="#FFA000" quantity="266.83887"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="279.7053"/>
          <sld:ColorMapEntry color="#FF0000" quantity="308.17896" label="308.17896  (ml/5km x 5km pixel)"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>