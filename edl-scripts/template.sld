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
	<ColorMapEntry color="#CCCCFF" quantity="MIN_QUANTITY" label="MIN_LABEL"/>
        <ColorMapEntry color="#009999" quantity="10_QUANTITY" label="10_LABEL"/>
        <ColorMapEntry color="#99FF66" quantity="20_QUANTITY" label="20_LABEL"/>
        <ColorMapEntry color="#FFFF66" quantity="30_QUANTITY" label="30_LABEL"/>
        <ColorMapEntry color="#FFFF00" quantity="40_QUANTITY" label="40_LABEL"/>
        <ColorMapEntry color="#FF9900" quantity="50_QUANTITY" label="50_LABEL"/>
        <ColorMapEntry color="#FF6600" quantity="60_QUANTITY" label="60_LABEL"/>
        <ColorMapEntry color="#FF6666" quantity="70_QUANTITY" label="70_LABEL"/>
        <ColorMapEntry color="#FF3300" quantity="80_QUANTITY" label="80_LABEL"/>
        <ColorMapEntry color="#CC33FF" quantity="90_QUANTITY" label="90_LABEL"/>
        <ColorMapEntry color="#FF33FF" quantity="MAX_QUANTITY" label="MAX_LABEL"/>
	<!--Higher-->
    </ColorMap>
</RasterSymbolizer>

</Rule>
</FeatureTypeStyle>
</UserStyle>
</UserLayer>
</StyledLayerDescriptor>
