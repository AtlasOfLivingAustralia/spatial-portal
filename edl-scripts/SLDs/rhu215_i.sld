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
          <sld:ColorMapEntry color="#009999" quantity="9.2763"/>
          <sld:ColorMapEntry color="#99FF66" quantity="18.5526"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="27.8289"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="37.1052"/>
          <sld:ColorMapEntry color="#FF9900" quantity="46.3815"/>
          <sld:ColorMapEntry color="#FF6600" quantity="55.6578"/>
          <sld:ColorMapEntry color="#FF6666" quantity="64.9341"/>
          <sld:ColorMapEntry color="#FF3300" quantity="74.2104"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="83.4867"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="92.763"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
