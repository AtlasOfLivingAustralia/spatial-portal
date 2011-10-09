This patch adds a wkt parameter to ssplex, http://sourceforge.net/projects/ssplex/.  Apply after SSP-2_0-RC4-ssplex-1_0.patch.

Use POLYGON and MULTIPOLYGON areas with the requirements:
- substitute ':' for ' ' as the longitude latitude delimeter.
- no whitespace.

Example {!spatial wkt=POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))}
