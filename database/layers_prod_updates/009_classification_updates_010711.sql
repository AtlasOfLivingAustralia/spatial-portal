\set ON_ERROR_STOP 1
begin;
update layers set classification1 = 'Area Management', classification2 = 'Biodiversity' where name = 'ibra_merged';
update layers set classification1 = 'Area Management', classification2 = 'Biodiversity' where name = 'ibra_sub_merged';
update layers set classification1 = 'Area Management', classification2 = 'Biodiversity' where name = 'imcra4_pb';
update layers set classification2 = 'Status' where name = 'ALA-SPATIAL_layer_occurrence_av_4';
update layers set classification2 = 'Status' where name = 'ALA-SPATIAL_layer_species_av_4';
commit;
