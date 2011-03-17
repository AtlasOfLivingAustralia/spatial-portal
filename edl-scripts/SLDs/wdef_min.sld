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
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="0.0"/>
          <sld:ColorMapEntry color="#009999" quantity="8.7993"/>
          <sld:ColorMapEntry color="#99FF66" quantity="17.5986"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="26.3979"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="35.1972"/>
          <sld:ColorMapEntry color="#FF9900" quantity="43.9965"/>
          <sld:ColorMapEntry color="#FF6600" quantity="52.7958"/>
          <sld:ColorMapEntry color="#FF6666" quantity="61.5951"/>
          <sld:ColorMapEntry color="#FF3300" quantity="70.3944"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="79.1937"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="87.993"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
