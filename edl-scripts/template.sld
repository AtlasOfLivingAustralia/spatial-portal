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
   

     	<ColorMapEntry color="#ffffff" quantity="-9999.0"/>
     	
 	<!--<ColorMapEntry color="#0000FF" quantity="0.0"/>
 	<ColorMapEntry color="#00CCFF" quantity="2500.0"/>
 	<ColorMapEntry color="#CC66CC" quantity="5000.0"/>
 	<ColorMapEntry color="#FF6600" quantity="30000.0"/>-->
 	<ColorMapEntry color="#99FF66" quantity="MIN"/>
        <ColorMapEntry color="#FF0000" quantity="MAX"/> 

    </ColorMap>
</RasterSymbolizer>

</Rule>
</FeatureTypeStyle>
</UserStyle>
</UserLayer>
</StyledLayerDescriptor>
