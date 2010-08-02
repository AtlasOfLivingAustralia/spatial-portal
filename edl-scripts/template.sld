<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd" version="1.0.0">
<UserLayer>
<Name>raster_color</Name>
<LayerFeatureConstraints>
<FeatureTypeConstraint/>
</LayerFeatureConstraints>
<UserStyle>
<Name>raster</Name>
<Title>A very simple color map</Title>
<Abstract>A very basic color map</Abstract>
<FeatureTypeStyle>
<FeatureTypeName>Feature</FeatureTypeName>
<Rule>
<RasterSymbolizer>
<ContrastEnhancement>
   <Normalise/>
</ContrastEnhancement>

    <Opacity>1.0</Opacity>
    <ChannelSelection>
        <GrayChannel>
        <SourceChannelName>1</SourceChannelName>
        </GrayChannel>
    </ChannelSelection>
    <ColorMap extended="true">
 	<!--Lower-->
	<!--<ColorMapEntry color="#CCCCFF" quantity="MIN"/>-->
        <ColorMapEntry color="#009999" quantity="TEN"/>
        <ColorMapEntry color="#99FF66" quantity="TWENTY"/>
        <ColorMapEntry color="#FFFF66" quantity="THIRTY"/>
        <ColorMapEntry color="#FFFF00" quantity="FOURTY"/>
        <ColorMapEntry color="#FF9900" quantity="FIFTY"/>
        <ColorMapEntry color="#FF6600" quantity="SIXTY"/>
        <ColorMapEntry color="#FF6666" quantity="SEVENTY"/>
        <ColorMapEntry color="#FF3300" quantity="EIGHTY"/>
        <ColorMapEntry color="#CC33FF" quantity="NINETY"/>
        <ColorMapEntry color="#FF33FF" quantity="MAX"/>
	<!--Higher-->
    </ColorMap>
</RasterSymbolizer>

</Rule>
</FeatureTypeStyle>
</UserStyle>
</UserLayer>
</StyledLayerDescriptor>
