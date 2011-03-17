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
          <sld:ColorMapEntry color="#009999" quantity="1290.0837"/>
          <sld:ColorMapEntry color="#99FF66" quantity="2580.1674"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="3870.2511"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="5160.3348"/>
          <sld:ColorMapEntry color="#FF9900" quantity="6450.4185"/>
          <sld:ColorMapEntry color="#FF6600" quantity="7740.5022"/>
          <sld:ColorMapEntry color="#FF6666" quantity="9030.5859"/>
          <sld:ColorMapEntry color="#FF3300" quantity="10320.6696"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="11610.7533"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="12900.837"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
