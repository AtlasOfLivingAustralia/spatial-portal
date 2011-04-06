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
          <sld:ColorMapEntry color="#002DD0" quantity="69.39244" label="69.39244 km/day"/>
          <sld:ColorMapEntry color="#005BA2" quantity="108.69634"/>
          <sld:ColorMapEntry color="#008C73" quantity="123.21139"/>
          <sld:ColorMapEntry color="#00B944" quantity="129.79941"/>
          <sld:ColorMapEntry color="#00E716" quantity="139.6"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="151.14645"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="158.91162"/>
          <sld:ColorMapEntry color="#FFC814" quantity="163.59674"/>
          <sld:ColorMapEntry color="#FFA000" quantity="171.20296"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="180.41275"/>
          <sld:ColorMapEntry color="#FF0000" quantity="381.31326" label="381.31326 km/day"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>