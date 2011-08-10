begin;
update layers
set classification1 = 'Marine', classification2 = 'Bathymetry' where id in(924,848);
commit;
