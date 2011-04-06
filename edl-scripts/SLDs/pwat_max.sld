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
          <sld:ColorMapEntry color="#002DD0" quantity="5.8064513" label="5.8064513 "/>
          <sld:ColorMapEntry color="#005BA2" quantity="11.873552"/>
          <sld:ColorMapEntry color="#008C73" quantity="14.052129"/>
          <sld:ColorMapEntry color="#00B944" quantity="16.990412"/>
          <sld:ColorMapEntry color="#00E716" quantity="22.162695"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="27.00645"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="35.386627"/>
          <sld:ColorMapEntry color="#FFC814" quantity="47.51145"/>
          <sld:ColorMapEntry color="#FFA000" quantity="67.62942"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="80.99999"/>
          <sld:ColorMapEntry color="#FF0000" quantity="81.0" label="81.0 "/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>