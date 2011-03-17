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
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="0"/>
          <sld:ColorMapEntry color="#009999" quantity="1.9273"/>
          <sld:ColorMapEntry color="#99FF66" quantity="4.3386"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="6.7499"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="9.1612"/>
          <sld:ColorMapEntry color="#FF9900" quantity="11.5725"/>
          <sld:ColorMapEntry color="#FF6600" quantity="13.9838"/>
          <sld:ColorMapEntry color="#FF6666" quantity="16.3951"/>
          <sld:ColorMapEntry color="#FF3300" quantity="18.8064"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="21.2177"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="23.629"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>