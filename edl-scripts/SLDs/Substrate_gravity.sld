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
          <sld:ColorMapEntry color="#002DD0" quantity="-1629.715" label="-1629.715 "/>
          <sld:ColorMapEntry color="#005BA2" quantity="-684.6451"/>
          <sld:ColorMapEntry color="#008C73" quantity="-502.2426"/>
          <sld:ColorMapEntry color="#00B944" quantity="-362.5548"/>
          <sld:ColorMapEntry color="#00E716" quantity="-263.1332"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="-191.5"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="-131.7243"/>
          <sld:ColorMapEntry color="#FFC814" quantity="-71.62006"/>
          <sld:ColorMapEntry color="#FFA000" quantity="7.251504"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="131.1625"/>
          <sld:ColorMapEntry color="#FF0000" quantity="928.735" label="928.735 "/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>