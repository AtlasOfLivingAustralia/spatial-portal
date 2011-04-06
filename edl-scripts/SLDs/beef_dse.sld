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
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="-10000"/>
          <sld:ColorMapEntry color="#002DD0" quantity="0.0" label="0.0 dse"/>
          <sld:ColorMapEntry color="#005BA2" quantity="674068.0"/>
          <sld:ColorMapEntry color="#008C73" quantity="1348136.0"/>
          <sld:ColorMapEntry color="#00B944" quantity="2022204.0"/>
          <sld:ColorMapEntry color="#00E716" quantity="2696272.0"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="3370340.0"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="4044408.0"/>
          <sld:ColorMapEntry color="#FFC814" quantity="4718476.0"/>
          <sld:ColorMapEntry color="#FFA000" quantity="5392544.0"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="6066612.0"/>
          <sld:ColorMapEntry color="#FF0000" quantity="6740680.0" label="6740680.0 dse"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>