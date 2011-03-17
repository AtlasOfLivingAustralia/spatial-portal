<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <Name>speciesUncertainity</Name>
    <UserStyle>
      <Name>speciesUncertainity</Name>
      <Title>Uncertainity of Species Occurrence</Title>
      <Abstract>Uncertainity of Species Occurrence</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
  <Graphic>
    <Mark>
      <WellKnownName><ogc:Function name="env">
            <ogc:Literal>name</ogc:Literal>
            <ogc:Literal>circle</ogc:Literal>
         </ogc:Function>
      </WellKnownName>
      <Fill>
        <CssParameter name="fill">#000000</CssParameter>
        <CssParameter name="fill-opacity">0.4</CssParameter>
      </Fill>
    </Mark>
    <Size>
       10
    </Size>
  </Graphic>
</PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>