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
          <sld:ColorMapEntry color="#009999" quantity="10120.0976"/>
          <sld:ColorMapEntry color="#99FF66" quantity="20232.7092"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="30345.3208"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="40457.9324"/>
          <sld:ColorMapEntry color="#FF9900" quantity="50570.544"/>
          <sld:ColorMapEntry color="#FF6600" quantity="60683.1556"/>
          <sld:ColorMapEntry color="#FF6666" quantity="70795.7672"/>
          <sld:ColorMapEntry color="#FF3300" quantity="80908.3788"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="91020.9904"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="101133.602"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
