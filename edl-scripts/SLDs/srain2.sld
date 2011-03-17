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
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="-37.667"/>
          <sld:ColorMapEntry color="#009999" quantity="-33.8462"/>
          <sld:ColorMapEntry color="#99FF66" quantity="-30.0254"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="-26.2046"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="-22.3838"/>
          <sld:ColorMapEntry color="#FF9900" quantity="-18.563"/>
          <sld:ColorMapEntry color="#FF6600" quantity="-14.7422"/>
          <sld:ColorMapEntry color="#FF6666" quantity="-10.9214"/>
          <sld:ColorMapEntry color="#FF3300" quantity="-7.1006"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="-3.2798"/>
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="0"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="0.541"/>
          
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>