<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <Name>world_10_bio01</Name>
    <UserStyle>
      <Name>alastyles</Name>
      <Title>Worldclim Annual Mean Temperature</Title>
      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <ColorMap type="intervals" extended="true">
              <ColorMapEntry color="#FFFFFF" quantity="-9999" opacity="0.0" />
              <ColorMapEntry color="#FFFFFF" quantity="0.0000" opacity="1" />
              <ColorMapEntry color="#008000" quantity="10.0" opacity="1"/>
              <ColorMapEntry color="#00FF00" quantity="15.0" opacity="1" />
              <ColorMapEntry color="#FFFF00" quantity="20.0" opacity="1" />
              <ColorMapEntry color="#FF9600" quantity="24.0" opacity="1" />
              <ColorMapEntry color="#FF0000" quantity="30.0" opacity="1" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>