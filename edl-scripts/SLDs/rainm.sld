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
          <sld:ColorMapEntry color="#009999" quantity="54.2381"/>
          <sld:ColorMapEntry color="#99FF66" quantity="108.4762"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="162.7143"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="216.9524"/>
          <sld:ColorMapEntry color="#FF9900" quantity="271.1905"/>
          <sld:ColorMapEntry color="#FF6600" quantity="325.4286"/>
          <sld:ColorMapEntry color="#FF6666" quantity="379.6667"/>
          <sld:ColorMapEntry color="#FF3300" quantity="433.9048"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="488.1429"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="542.381"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
