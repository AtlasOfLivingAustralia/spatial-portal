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
          <sld:ColorMapEntry color="#002DD0" quantity="33.388775" label="33.388775 %"/>
          <sld:ColorMapEntry color="#005BA2" quantity="36.393658"/>
          <sld:ColorMapEntry color="#008C73" quantity="37.51402"/>
          <sld:ColorMapEntry color="#00B944" quantity="38.50792"/>
          <sld:ColorMapEntry color="#00E716" quantity="39.64845"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="41.58744"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="44.159084"/>
          <sld:ColorMapEntry color="#FFC814" quantity="46.832928"/>
          <sld:ColorMapEntry color="#FFA000" quantity="50.99152"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="58.57221"/>
          <sld:ColorMapEntry color="#FF0000" quantity="89.05581" label="89.05581 %"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>