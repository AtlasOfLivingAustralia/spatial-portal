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
          <sld:ColorMapEntry color="#FF33FF" quantity="-486.800"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="-431.6076"/>
          <sld:ColorMapEntry color="#FF3300" quantity="-376.4152"/>
          <sld:ColorMapEntry color="#FF6666" quantity="-321.2228"/>
          <sld:ColorMapEntry color="#FF6600" quantity="-266.0304"/>
          <sld:ColorMapEntry color="#FF9900" quantity="-210.838"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="-155.6456"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="-100.4532"/>
          <sld:ColorMapEntry color="#99FF66" quantity="-45.2608"/>
           <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="0"/>
          <sld:ColorMapEntry color="#009999" quantity="9.9316"/>
          <sld:ColorMapEntry color="#CCCCFF" quantity="65.124"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>