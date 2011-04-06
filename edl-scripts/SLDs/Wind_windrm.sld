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
          <sld:ColorMapEntry color="#002DD0" quantity="87.8134" label="87.8134 km/day"/>
          <sld:ColorMapEntry color="#005BA2" quantity="130.41232"/>
          <sld:ColorMapEntry color="#008C73" quantity="146.5245"/>
          <sld:ColorMapEntry color="#00B944" quantity="156.90138"/>
          <sld:ColorMapEntry color="#00E716" quantity="169.16783"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="182.14465"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="193.03635"/>
          <sld:ColorMapEntry color="#FFC814" quantity="200.1302"/>
          <sld:ColorMapEntry color="#FFA000" quantity="212.47981"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="227.63788"/>
          <sld:ColorMapEntry color="#FF0000" quantity="419.5367" label="419.5367 km/day"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>