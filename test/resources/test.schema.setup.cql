CREATE KEYSPACE IF NOT EXISTS lcmap_landsat_unit
WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1};


CREATE TABLE IF NOT EXISTS lcmap_landsat_unit.sources (
    id text,
    uri text static,
    checksum text static,
    progress_at timestamp,
    progress_name text,
    progress_desc text,
    ubid text,
    PRIMARY KEY (id, progress_at)
)
WITH COMPRESSION = { 'sstable_compression' : 'LZ4Compressor' }
AND  COMPACTION = { 'class' :  'LeveledCompactionStrategy' };


CREATE TABLE IF NOT EXISTS lcmap_landsat_unit.conus (
    x int,
    y int,
    ubid text,
    acquired timestamp,
    data blob,
    source text,
    PRIMARY KEY ((x, y, ubid), acquired)
)
WITH COMPRESSION = { 'sstable_compression' : 'LZ4Compressor' }
AND  COMPACTION = { 'class' :  'LeveledCompactionStrategy' };


CREATE TABLE IF NOT EXISTS lcmap_landsat_unit.chip_specs (
    name text,
    ubid text,
    tags set<text>,
    wkt text static,
    pixel_x float static,
    pixel_y float static,
    chip_x int,
    chip_y int,
    shift_x float,
    shift_y float,
    resample_method text,
    satellite text,
    instrument text,
    band_product text,
    band_category text,
    band_name text,
    band_long_name text,
    band_short_name text,
    band_spectrum list<double>,
    data_fill int,
    data_range list<double>,
    data_scale float,
    data_type text,
    data_units text,
    data_shape list<int>,
    data_mask map<int, text>,
    PRIMARY KEY (name, ubid));
