CREATE TABLE station
(
    id INTEGER PRIMARY KEY,
    source_id TEXT NOT NULL,
    external_id TEXT NOT NULL,
    name TEXT NOT NULL,
    lat REAL NOT NULL CHECK(lat BETWEEN -90 AND 90),
    lon REAL NOT NULL CHECK(lon BETWEEN -180 AND 180),
    source_type TEXT NOT NULL CHECK(source_type IN ('REFERENCE', 'LOW_COST')),
    active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)),

    UNIQUE (source_id, external_id)
) STRICT;

CREATE INDEX idx_station_lat_lon ON station (lat, lon);

CREATE TABLE measurement
(
    id INTEGER PRIMARY KEY,
    station_id INTEGER NOT NULL REFERENCES station(id),
    parameter TEXT NOT NULL,
    value REAL NOT NULL,
    unit TEXT NOT NULL,
    measured_at INTEGER NOT NULL,
    fetched_at INTEGER NOT NULL,

    UNIQUE (station_id, parameter, measured_at)
) STRICT;
