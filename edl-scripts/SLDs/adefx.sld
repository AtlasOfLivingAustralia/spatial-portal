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
          <sld:ColorMapEntry color="#002DD0" quantity="-189.6" label="-189.6 mm"/>
          <sld:ColorMapEntry color="#005BA2" quantity="-133.44113"/>
          <sld:ColorMapEntry color="#008C73" quantity="-110.025764"/>
          <sld:ColorMapEntry color="#00B944" quantity="-94.0"/>
          <sld:ColorMapEntry color="#00E716" quantity="-80.9"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="-64.85192"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="-47.699997"/>
          <sld:ColorMapEntry color="#FFC814" quantity="-25.955444"/>
          <sld:ColorMapEntry color="#FFA000" quantity="4.6073837"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="59.827145"/>
          <sld:ColorMapEntry color="#FF0000" quantity="1087.9867" label="1087.9867 mm"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>