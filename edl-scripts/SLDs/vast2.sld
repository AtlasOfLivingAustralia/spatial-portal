<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<NamedLayer>
		<Name> vast </Name>
		<UserStyle>
			<FeatureTypeStyle>
				<FeatureTypeName>Feature</FeatureTypeName>
        
        <!-- Bare  -->
				<Rule>
					<Title>Bare</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>0</ogc:Literal>
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

        <!-- Residual -->
				<Rule>
					<Title>Residual</Title>
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

        <!-- Modified  -->
				<Rule>
					<Title>Modified</Title>
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

        <!-- Transformed -->
				<Rule>
					<Title>Transformed</Title>
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
        
        <!-- Replaced  -->
				<Rule>
					<Title>Replaced</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>5</ogc:Literal>
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
        
        <!-- Removed -->
				<Rule>
					<Title>Removed</Title>
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
        
			</FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>