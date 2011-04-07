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
          <sld:ColorMapEntry color="#008C73" quantity="5.966007"/>
          <sld:ColorMapEntry color="#00B944" quantity="9.1468935"/>
          <sld:ColorMapEntry color="#00E716" quantity="12.325818"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="15.003586"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="18.172325"/>
          <sld:ColorMapEntry color="#FFC814" quantity="21.689098"/>
          <sld:ColorMapEntry color="#FFA000" quantity="25.735619"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="33.712574"/>
          <sld:ColorMapEntry color="#FF0000" quantity="87.99275" label="87.99275 mm"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>