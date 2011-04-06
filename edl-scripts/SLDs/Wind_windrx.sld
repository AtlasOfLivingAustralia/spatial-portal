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
          <sld:ColorMapEntry color="#002DD0" quantity="106.791725" label="106.791725 km/day"/>
          <sld:ColorMapEntry color="#005BA2" quantity="149.2013"/>
          <sld:ColorMapEntry color="#008C73" quantity="168.6618"/>
          <sld:ColorMapEntry color="#00B944" quantity="181.2"/>
          <sld:ColorMapEntry color="#00E716" quantity="195.28447"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="208.41548"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="222.92526"/>
          <sld:ColorMapEntry color="#FFC814" quantity="236.86705"/>
          <sld:ColorMapEntry color="#FFA000" quantity="252.5"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="271.39993"/>
          <sld:ColorMapEntry color="#FF0000" quantity="515.68774" label="515.68774 km/day"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>