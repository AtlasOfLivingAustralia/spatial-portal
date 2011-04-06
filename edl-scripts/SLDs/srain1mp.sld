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
          <sld:ColorMapEntry color="#002DD0" quantity="0.06483808" label="0.06483808 dimensionless"/>
          <sld:ColorMapEntry color="#005BA2" quantity="0.5900743"/>
          <sld:ColorMapEntry color="#008C73" quantity="1.0"/>
          <sld:ColorMapEntry color="#00B944" quantity="1.5471698"/>
          <sld:ColorMapEntry color="#00E716" quantity="1.9630945"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="2.4535184"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="3.3744402"/>
          <sld:ColorMapEntry color="#FFC814" quantity="5.0833335"/>
          <sld:ColorMapEntry color="#FFA000" quantity="11.533334"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="32.646214"/>
          <sld:ColorMapEntry color="#FF0000" quantity="545.0" label="545.0 dimensionless"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>