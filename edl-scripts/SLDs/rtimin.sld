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
          <sld:ColorMapEntry color="#002DD0" quantity="-5.8200073" label="-5.8200073 degrees C"/>
          <sld:ColorMapEntry color="#005BA2" quantity="-5.119995"/>
          <sld:ColorMapEntry color="#008C73" quantity="-4.8900146"/>
          <sld:ColorMapEntry color="#00B944" quantity="-4.6700134"/>
          <sld:ColorMapEntry color="#00E716" quantity="-4.4299927"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="-4.169983"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="-3.869995"/>
          <sld:ColorMapEntry color="#FFC814" quantity="-3.600006"/>
          <sld:ColorMapEntry color="#FFA000" quantity="-3.2700195"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="-2.7700195"/>
          <sld:ColorMapEntry color="#FF0000" quantity="-0.8500061" label="-0.8500061 degrees C"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>