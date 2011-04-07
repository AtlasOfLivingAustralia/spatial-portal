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
		
<ColorMapEntry color = "#002DD0" quantity="MIN_QUANTITY" label="MIN_LABEL"/>
<ColorMapEntry color = "#005BA2" quantity="Q1_QUANTITY"/>
<ColorMapEntry color = "#008C73" quantity="Q2_QUANTITY"/>
<ColorMapEntry color = "#00B944" quantity="Q3_QUANTITY"/>
<ColorMapEntry color = "#00E716" quantity="Q4_QUANTITY"/>
<ColorMapEntry color = "#A0FF00" quantity="Q5_QUANTITY"/>
<ColorMapEntry color = "#FFFF00" quantity="Q6_QUANTITY"/>
<ColorMapEntry color = "#FFC814" quantity="Q7_QUANTITY"/>
<ColorMapEntry color = "#FFA000" quantity="Q8_QUANTITY"/>
<ColorMapEntry color = "#FF5B00" quantity="Q9_QUANTITY"/>
<ColorMapEntry color = "#FF0000" quantity="MAX_QUANTITY" label="MAX_LABEL"/>

	<!--Higher-->
    </ColorMap>
</RasterSymbolizer>

</Rule>
</FeatureTypeStyle>
</UserStyle>
</UserLayer>
</StyledLayerDescriptor>
