<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<NamedLayer>
		<Name> tenure08 </Name>
		<UserStyle>
			<FeatureTypeStyle>
				<FeatureTypeName>Feature</FeatureTypeName>

        <!-- Multiple use forests -->
				<Rule>
					<Title>Multiple use forests</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>1</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#EE002D</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#EE002D</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Nature conservation areas  -->
				<Rule>
					<Title>Nature conservation areas</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>2</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#1C4E29</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#1C4E29</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- No data -->
				<Rule>
					<Title>No data</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>3</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#008548</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#008548</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Ocean -->
				<Rule>
					<Title>Ocean</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>4</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#62C057</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#62C057</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Other crown land -->
				<Rule>
					<Title>Other crown land</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>5</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#C1D6C8</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#C1D6C8</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Private freehold -->
				<Rule>
					<Title>Private freehold</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>6</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#8EAE4E</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#8EAE4E</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Private leasehold -->
				<Rule>
					<Title>Private leasehold</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>7</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#8EBC91</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#8EBC91</ogc:Literal>
							</CssParameter>


						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Reserved crown land -->
				<Rule>
					<Title>Reserved crown land</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>8</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#44C0A0</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#44C0A0</ogc:Literal>
							</CssParameter>


						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
         <!-- Water production -->
				<Rule>
					<Title>Water production</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>9</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#B4DEB3</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#B4DEB3</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
			</FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>