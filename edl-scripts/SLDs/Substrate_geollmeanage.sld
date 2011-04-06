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
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="-999"/>
          <sld:ColorMapEntry color="#002DD0" quantity="-2.2403321" label="-2.2403321 log my"/>
          <sld:ColorMapEntry color="#005BA2" quantity="-0.33984628"/>
          <sld:ColorMapEntry color="#008C73" quantity="0.11159852"/>
          <sld:ColorMapEntry color="#00B944" quantity="0.11193429"/>
          <sld:ColorMapEntry color="#00E716" quantity="1.5142788"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="1.5152113"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="2.0737183"/>
          <sld:ColorMapEntry color="#FFC814" quantity="2.5173278"/>
          <sld:ColorMapEntry color="#FFA000" quantity="2.7222226"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="3.2582781"/>
          <sld:ColorMapEntry color="#FF0000" quantity="3.5502284" label="3.5502284 log my"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>