<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<NamedLayer>
		<Name> landcover </Name>
		<UserStyle>
			<FeatureTypeStyle>
				<FeatureTypeName>Feature</FeatureTypeName>

        <!-- Native forests and woodlands -->
				<Rule>
					<Title>Native forests and woodlands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>1</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#9666CD</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#9666CD</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Native shrublands and heathlands  -->
				<Rule>
					<Title>Native shrublands and heathlands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>2</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#C9BEFF</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#C9BEFF</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Native grasslands and minimally modified pastures  -->
				<Rule>
					<Title>Native grasslands and minimally modified pastures</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>3</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#DE87DD</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#DE87DD</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Horticultural trees and shrubs  -->
				<Rule>
					<Title>Horticultural trees and shrubs</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>4</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#FFFFE5</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#FFFFE5</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Perennial crops  -->
				<Rule>
					<Title>Perennial crops</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>5</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#298944</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#298944</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Annual crops and highly modified pastures  -->
				<Rule>
					<Title>Annual crops and highly modified pastures</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>6</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#ADFFB5</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#ADFFB5</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Plantation (hardwood) -->
				<Rule>
					<Title>Plantation (hardwood)</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>7</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#FF930F</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#FF930F</ogc:Literal>
							</CssParameter>


						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Plantation (softwood/mixed) -->
				<Rule>
					<Title>Plantation (softwood/mixed)</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>8</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#FFFF00</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#FFFF00</ogc:Literal>
							</CssParameter>


						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
         <!-- Bare -->
				<Rule>
					<Title>Bare</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>9</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#AB8778</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#AB8778</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
         <!-- Ephemeral and Permanent Water Features -->
				<Rule>
					<Title>Ephemeral and Permanent Water Features</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>10</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#C9B854</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#C9B854</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
         <!-- Built-up -->
				<Rule>
					<Title>Built-up</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>11</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#9C542E</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#9C542E</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
			</FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>