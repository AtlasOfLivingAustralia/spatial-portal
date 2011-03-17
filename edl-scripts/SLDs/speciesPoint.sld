<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <Name>speciesPoint</Name>
    <UserStyle>
      <Name>speciesPoint</Name>
      <Title>Points of Species Occurrence</Title>
      <Abstract>Manhattan points of interest</Abstract>
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
        <CssParameter name="fill">
         #<ogc:Function name="env">
            <ogc:Literal>color</ogc:Literal>
            <ogc:Literal>FF00FF</ogc:Literal>
         </ogc:Function>
        </CssParameter>
        <CssParameter name="fill-opacity">
         <ogc:Function name="env">
            <ogc:Literal>opacity</ogc:Literal>
            <ogc:Literal>0.4</ogc:Literal>
         </ogc:Function>
        </CssParameter>
      </Fill>
    </Mark>
    <Size>
       <ogc:Function name="env">
          <ogc:Literal>size</ogc:Literal>
          <ogc:Literal>6</ogc:Literal>
       </ogc:Function>
    </Size>
  </Graphic>
</PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>