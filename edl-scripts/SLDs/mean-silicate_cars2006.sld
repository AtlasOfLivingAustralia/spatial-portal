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
          <sld:ColorMapEntry color="#002DD0" quantity="-30963" label="-30963 umol/l"/>
          <sld:ColorMapEntry color="#005BA2" quantity="-27866.7"/>
          <sld:ColorMapEntry color="#008C73" quantity="-24770.4"/>
          <sld:ColorMapEntry color="#00B944" quantity="-21674.1"/>
          <sld:ColorMapEntry color="#00E716" quantity="-18577.8"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="-15481.5"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="-12385.2"/>
          <sld:ColorMapEntry color="#FFC814" quantity="-9288.9"/>
          <sld:ColorMapEntry color="#FFA000" quantity="-6192.6"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="-3096.3"/>
          <sld:ColorMapEntry color="#FF0000" quantity="-1" label="0 umol/l"/>
          <sld:ColorMapEntry color="#FF33FF" opacity="0" quantity="0.000"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>