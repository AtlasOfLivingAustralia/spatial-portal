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
          <sld:ColorMapEntry color="#002DD0" quantity="0.0" label="0.0 mm"/>
          <sld:ColorMapEntry color="#005BA2" quantity="92.912865"/>
          <sld:ColorMapEntry color="#008C73" quantity="119.19092"/>
          <sld:ColorMapEntry color="#00B944" quantity="139.81516"/>
          <sld:ColorMapEntry color="#00E716" quantity="160.52818"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="174.3088"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="187.72319"/>
          <sld:ColorMapEntry color="#FFC814" quantity="205.12581"/>
          <sld:ColorMapEntry color="#FFA000" quantity="226.8012"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="253.71088"/>
          <sld:ColorMapEntry color="#FF0000" quantity="379.72562" label="379.72562 mm"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>