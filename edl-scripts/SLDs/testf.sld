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
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="-10000.0"/>
          <sld:ColorMapEntry color="#002DD0" quantity="MIN_QUANTITY" label="MIN_LABEL"/>
          <sld:ColorMapEntry color="#005BA2" quantity="Q1_QUANTITY"/>
          <sld:ColorMapEntry color="#008C73" quantity="Q2_QUANTITY"/>
          <sld:ColorMapEntry color="#00B944" quantity="Q3_QUANTITY"/>
          <sld:ColorMapEntry color="#00E716" quantity="Q4_QUANTITY"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="Q5_QUANTITY"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="Q6_QUANTITY"/>
          <sld:ColorMapEntry color="#FFC814" quantity="Q7_QUANTITY"/>
          <sld:ColorMapEntry color="#FFA000" quantity="Q8_QUANTITY"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="Q9_QUANTITY"/>
          <sld:ColorMapEntry color="#FF0000" quantity="MAX_QUANTITY" label="MAX_LABEL"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>