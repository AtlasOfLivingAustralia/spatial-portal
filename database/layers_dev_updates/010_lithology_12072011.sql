begin;

-- lith_geologicalunitpolygons1m
INSERT INTO layers
(
  id,
  "name",
  description,
  "type",
  source,
  path,
  extents,
  minlatitude,
  minlongitude,
  maxlatitude,
  maxlongitude,
  notes,
  enabled,
  displayname,
  displaypath,
  scale,
  environmentalvaluemin,
  environmentalvaluemax,
  environmentalvalueunits,
  lookuptablepath,
  metadatapath,
  classification1,
  classification2,
  uid,
  mddatest,
  citation_date,
  datalang,
  mdhrlv,
  respparty_role,
  licence_level,
  licence_link,
  licence_notes,
  source_link
)
VALUES (
923,
'lith_geologicalunitpolygons1m', 
'Surface Geology of Australia 1:1,000,000 Scale, 2010 Edition', 
'Contextual', 
'GA', 
'/data/ala/data/shapefiles/surfgeol2010/surfacegeology_1M_shapefiles', 
NULL, 
-54.778, 
72.577, 
-9.178, 
167.998, 
'The Surface Geology of Australia (2010 edition) is a seamless national coverage of outcrop and surficial geology, compiled for us
e at or around 1:1 000 000 scale.  The data maps outcropping bedrock geology and unconsolidated or poorly consolidated regolith m
aterial covering bedrock.  Geological units are represented as polygon and line geometries, and are attributed with information r
egarding stratigraphic nomenclature and parentage, age, lithology, and primary data source.  The dataset also contains geological
 contacts, structural features such as faults and shears, and miscellaneous supporting lines like the boundaries of water and ice
 bodies.

The dataset has been compiled from merging the seven State and Territory 1:1 000 000 scale surface geology datasets released by G
eoscience Australia between 2006 and 2008, correcting errors and omissions identified in those datasets, addition of some offshor
e island territories, and updating stratigraphic attribute information to the best available in 2010 from the Australian Stratigr
aphic Units Database (http://www.ga.gov.au/oracle/stratnames/index.jsp).  The map data were compiled largely from simplifying and
 edgematching existing 1:250 000 scale geological maps.  Where these maps were not current, more recent source maps, ranging in s
cale from 1:50 000 to 1:1 000 000 were used.  In some areas where the only available geological maps were quite old and poorly lo
cated, some repositioning of mapping using recent satellite imagery or geophysics was employed.

This data is freely available from Geoscience Australia under the Creative Commons Attribution 2.5 Australia Licence.


It is recommended that these data be referred to as: 

Raymond, O.L., Retter, A.J., (editors), 2010.  Surface geology of Australia 1:1,000,000 scale, 2010 edition [Digital Dataset]
Geoscience Australia, Commonwealth of Australia, Canberra.  http://www.ga.gov.au

Specialised Geographic Information System (GIS) software is required to view this data.

Descriptions of MAP_SYMB attribute field:
MAP_SYMB format  = Drxy

1. D = unit age.  Two letters may be used for units spanning for than one age periods.  

Cenozoic                Cz
Quaternary              Q
Mesozoic                Mz
Cretaceous              K
Jurassic                J
Triassic                -R
Paleozoic               Pz
Permian                 P
Carboniferous           C
Devonian                D
Silurian                S
Ordovician              O
Cambrian                -C
Proterozoic             -P
Neoproterozoic          N
Mesoproterozoic         M
Paleoproterozoic        L
Archean                 A

2. r = gross rock descriptor.  A one letter code to reflect the broad lithological composition of the unit

IGNEOUS                                           EXAMPLES
g  felsic to intermediate intrusive               granite, granodiorite, tonalite, monzonite, diorite, syenite
d  mafic intrusive                                gabbro, dolerite, norite
f  felsic extrusive / high level intrusive        rhyolite, dacite, ignimbrite, pyroclastic rocks
a  intermediate extrusive / high level intrusive  andesite, trachyte, latite, pyroclastic rocks
b  mafic extrusive / high level intrusive         basalt, scoria, shoshonite, pyroclastic rocks
u  ultramafic undivided (intrusive & extrusive)   komatiite, high Mg basalt, pyroxenite, dunite, wehrlite
k  alkaline ultramafic                            kimberlite, lamprophyre, carbonatite

SEDIMENTARY
s  siliciclastic/undifferentiated sediment        shale, siltstone, sandstone, conglomerate, mudstone
j  volcanogenic sediment                          epiclastic sediments and breccias, greywacke, arkose
l  carbonate sediment                             limestone, marl, dolomite
c  non-carbonate chemical sediment                chert, evaporite, phosphorite, BIF
o  organic-rich rock                              coal, amber, oil shale

MIXED SEDIMENTARY & IGNEOUS
v  felsic & mafic volcanics  
i  felsic & mafic intrusives  
w  volcanics & sediments  

METAMORPHIC
y  low-medium grade meta clastic sediment             slate, phyllite, schist, quartzite
t  low-medium grade metabasite                  mafic schist, greenstone, amphibolite
r  low-medium grade metafelsite                 rhyolitic schist, meta-andesite
m  calc-silicate and marble                     meta carbonates and calcareous sediments
n  high grade metamorphic rock                  gneiss, granulite, migmatite
p  high-P metamorphic rock                      eclogite, blueschist
h  contact metamorphic rock                     hornfels, spotted slate
e  metamorphosed ultramafic rocks               serpentinite, talc schist, chlorite schist (no feldspars), tremolite schist, ultr
amafic amphibolite

OTHER
z  fault / shear rock                           mylonite, fault breccia, cataclasite, gouge
q  vein                                         quartz vein, carbonate vein
x                                               complex, melange, undivided, unknown

3.   xy = One or two letters to reflect the stratigraphic name of a unit.  Where practical, these letters reflect stratigraphic g
rouping or hierarchy.  For instance, formations within a named group should have letter symbols reflecting their parent group.

eg:   Tomkinson Creek Group -  Lsk
           Bootu Formation  -  Lskb', 
true,
'Surface Geology of Australia', 
'http://spatial-dev.ala.org.au/geoserver/gwc/service/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:lith_geologicalunitpolygons1m&format=image/png&styles=', 
'1:1,000,000 Scale', 
'', 
'', 
'', 
'', 
'http://www.ga.gov.au/', 
'Lithology',
'', 
'923', 
'2010-01', 
'2010', 
'eng', 
NULL, 
NULL, 
2,
'http://creativecommons.org/licenses/by/2.5/au/deed.en',
'This Geoscience Australia Data is available under the Creative Commons Attribution 2.5 Australia Licence

You are free to:
    - Share   -   to copy, adapt, distribute and transmit the work
    - Remix   -   to adapt the work

Under the following conditions:
-       Attribution - You must attribute the work as follows:

Copyright Commonwealth of Australia (Geoscience Australia) 2009 ',
'http://www.ga.gov.au'
);

commit;

