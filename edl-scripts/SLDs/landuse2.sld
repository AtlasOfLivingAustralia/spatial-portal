<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<NamedLayer>
		<Name> landuse </Name>
		<UserStyle>
			<FeatureTypeStyle>
				<FeatureTypeName>Feature</FeatureTypeName>

        <!-- Nature conservation -->
				<Rule>
					<Title>Nature conservation</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Nature conservation</ogc:Literal>
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

        <!-- Managed resource protected areas   -->
				<Rule>
					<Title>Managed resource protected areas</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Managed resource protected areas</ogc:Literal>
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

        <!-- Other minimal uses   -->
				<Rule>
					<Title>Other minimal uses</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Other minimal uses</ogc:Literal>
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
        
        <!-- Grazing of native pastures   -->
				<Rule>
					<Title>Grazing of native pastures</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Grazing of native pastures</ogc:Literal>
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
        
        <!-- Forestry   -->
				<Rule>
					<Title>Forestry</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Forestry</ogc:Literal>
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
        
        <!-- Plantation   -->
				<Rule>
					<Title>Plantation</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Plantation</ogc:Literal>
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
        
        <!-- Modified pastures   -->
				<Rule>
					<Title>Modified pastures</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Modified pastures</ogc:Literal>
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
        
        <!-- Cropping   -->
				<Rule>
					<Title>Cropping</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Cropping</ogc:Literal>
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
        
         <!-- Horticulture  -->
				<Rule>
					<Title>Horticulture</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Horticulture</ogc:Literal>
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
        
         <!-- Irrigated pastures and cropping  -->
				<Rule>
					<Title>Irrigated pastures and cropping</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Irrigated pastures and cropping</ogc:Literal>
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
        
         <!-- Irrigated horticulture  -->
				<Rule>
					<Title>Irrigated horticulture</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Irrigated horticulture</ogc:Literal>
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
        
        <!-- Intensive animal and plant production -->
				<Rule>
					<Title>Intensive animal and plant production</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Intensive animal and plant production</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#FFC9BE</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#FFC9BE</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Rural residential -->
				<Rule>
					<Title>Rural residential</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Rural residential</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#B2B2B2</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#B2B2B2</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Urban intensive uses -->
				<Rule>
					<Title>Urban intensive uses</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Urban intensive uses</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#FF0000</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#FF0000</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Mining and waste -->
				<Rule>
					<Title>Mining and waste</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Mining and waste</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#47828F</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#47828F</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Water -->
				<Rule>
					<Title>Water</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Water</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#0000FF</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#0000FF</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Land in transition -->
				<Rule>
					<Title>Land in transition</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>classname</ogc:PropertyName>
							<ogc:Literal>Land in transition</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#000000</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#000000</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

			</FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>