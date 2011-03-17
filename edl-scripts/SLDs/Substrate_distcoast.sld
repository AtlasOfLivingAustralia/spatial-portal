<?xml version="1.0" encoding="UTF-8"?>
<sld:UserStyle xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml">
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
          <sld:ColorMapEntry color="#009999" quantity="0.8422"/>
          <sld:ColorMapEntry color="#99FF66" quantity="1.6844"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="2.5266"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="3.3688"/>
          <sld:ColorMapEntry color="#FF9900" quantity="4.211"/>
          <sld:ColorMapEntry color="#FF6600" quantity="5.0532"/>
          <sld:ColorMapEntry color="#FF6666" quantity="5.8954"/>
          <sld:ColorMapEntry color="#FF3300" quantity="6.7376"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="7.5798"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="8.422"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
