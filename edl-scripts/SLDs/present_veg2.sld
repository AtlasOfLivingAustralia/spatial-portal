<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<NamedLayer>
		<Name> present veg </Name>
		<UserStyle>
			<FeatureTypeStyle>
				<FeatureTypeName>Feature</FeatureTypeName>

        <!-- Rainforest and vine thickets -->
				<Rule>
					<Title>Rainforest and vine thickets</Title>
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

        <!-- Eucalyptus tall open forest  -->
				<Rule>
					<Title>Eucalyptus tall open forest</Title>
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

        <!-- Eucalyptus open forest -->
				<Rule>
					<Title>Eucalyptus open forest</Title>
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
        
        <!-- Eucalyptus low open forest  -->
				<Rule>
					<Title>Eucalyptus low open forest</Title>
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
        
        <!-- Eucalyptus woodlands  -->
				<Rule>
					<Title>Eucalyptus woodlands</Title>
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
        
        <!-- Acacia forests and woodlands -->
				<Rule>
					<Title>Acacia forests and woodlands</Title>
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
        
        <!-- Callitris forests and woodlands -->
				<Rule>
					<Title>Callitris forests and woodlands</Title>
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
        
        <!-- Casuarina forests and woodlands -->
				<Rule>
					<Title>Casuarina forests and woodlands</Title>
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
        
         <!-- Melaleuca forests and woodlands -->
				<Rule>
					<Title>Melaleuca forests and woodlands</Title>
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
        
         <!-- Other forests and woodlands -->
				<Rule>
					<Title>Other forests and woodlands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>10</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#8FD4C7</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#8FD4C7</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
         <!-- Eucalyptus open woodlands -->
				<Rule>
					<Title>Eucalyptus open woodlands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>11</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#E3F3E8</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#E3F3E8</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Tropical eucalypt woodlands/grasslands -->
				<Rule>
					<Title>Tropical eucalypt woodlands/grasslands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>12</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#CAC0DD</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#CAC0DD</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Acacia open woodlands -->
				<Rule>
					<Title>Acacia open woodlands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>13</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#EEE397</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#EEE397</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Mallee woodlands and shrublands -->
				<Rule>
					<Title>Mallee woodlands and shrublands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>14</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#BDB775</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#BDB775</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>
        
        <!-- Low closed forest and tall closed shrubland -->
				<Rule>
					<Title>Low closed forest and tall closed shrubland</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>15</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#877338</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#877338</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Acacia shrublands -->
				<Rule>
					<Title>Acacia shrublands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>16</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#FABCBF</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#FABCBF</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Other shrublands -->
				<Rule>
					<Title>Other shrublands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>17</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#8A7267</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#8A7267</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Heathlands -->
				<Rule>
					<Title>Heathlands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>18</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#F89C81</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#F89C81</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Tussock grasslands -->
				<Rule>
					<Title>Tussock grasslands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>19</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#B8AC92</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#B8AC92</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Hummock grasslands -->
				<Rule>
					<Title>Hummock grasslands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>20</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#FEFAE1</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#FEFAE1</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Other grasslands, herblands, sedgelands and rushlands -->
				<Rule>
					<Title>Other grasslands, herblands, sedgelands and rushlands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>21</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#FAE4AE</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#FAE4AE</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Chenopod shrublands, samphire shrubs and forblands -->
				<Rule>
					<Title>Chenopod shrublands, samphire shrubs and forblands</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>22</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#FCE4DD</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#FCE4DD</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Mangroves -->
				<Rule>
					<Title>Mangroves</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>23</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#0FA5A9</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#0FA5A9</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Inland aquatic - freshwater, salt lakes, lagoons -->
				<Rule>
					<Title>Inland aquatic - freshwater, salt lakes, lagoons</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>24</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#486DB0</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#486DB0</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- cleared, non-native vegetation, buildings -->
				<Rule>
					<Title>cleared, non-native vegetation, buildings</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>25</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#FFFFFF</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#FFFFFF</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Unclassified native vegetation -->
				<Rule>
					<Title>Unclassified native vegetation</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>26</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#4F504F</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#4F504F</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Naturally bare - sand, rock, claypan, mudflats -->
				<Rule>
					<Title>Naturally bare - sand, rock, claypan, mudflats</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>27</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#CCCBCB</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#CCCBCB</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Sea and estuaries -->
				<Rule>
					<Title>Sea and estuaries</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>28</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#9BDCEF</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#9BDCEF</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Regrowth, modified native vegetation -->
				<Rule>
					<Title>Regrowth, modified native vegetation</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>29</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#9B9B9B</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#9B9B9B</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

        <!-- Unknown/no data -->
				<Rule>
					<Title>Unknown/no data</Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>dn</ogc:PropertyName>
							<ogc:Literal>99</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">
								<ogc:Literal>#EBEBEB</ogc:Literal>
							</CssParameter>
							<CssParameter name="fill-opacity">
								<ogc:Literal>1.0</ogc:Literal>
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">
								<ogc:Literal>#EBEBEB</ogc:Literal>
							</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

			</FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>