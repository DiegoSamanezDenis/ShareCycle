CREATE TABLE station (
    station_id        BINARY(16)   NOT NULL,
    station_name      VARCHAR(255) NOT NULL,
    station_status    SMALLINT     NOT NULL,
    station_latitude  DOUBLE       NOT NULL,
    station_longtitude DOUBLE      NOT NULL,
    bikes_docked      INT          NOT NULL DEFAULT 0,
    station_capacity  INT          NOT NULL,
    address           VARCHAR(255) NOT NULL,
    CONSTRAINT pk_station PRIMARY KEY (station_id),
    CONSTRAINT ck_station_status CHECK (station_status BETWEEN 0 AND 3),
    CONSTRAINT ck_station_capacity CHECK (station_capacity >= 0),
    CONSTRAINT ck_station_bikes_nonneg CHECK (bikes_docked >= 0),
    CONSTRAINT ck_station_bikes_capacity CHECK (bikes_docked <= station_capacity)
);

CREATE TABLE bike (
    bike_id             BINARY(16)  NOT NULL,
    bike_type           SMALLINT    NOT NULL,
    bike_status         SMALLINT    NOT NULL,
    reservation_expiry  DATETIME    NULL,
    current_station_id  BINARY(16)  NULL,
    CONSTRAINT pk_bike PRIMARY KEY (bike_id),
    CONSTRAINT ck_bike_type CHECK (bike_type BETWEEN 0 AND 1),
    CONSTRAINT ck_bike_status CHECK (bike_status BETWEEN 0 AND 3),
    CONSTRAINT ck_bike_reservation_expiry CHECK (
        (bike_status = 1 AND reservation_expiry IS NOT NULL) OR
        (bike_status <> 1 AND reservation_expiry IS NULL)
    )
);

CREATE INDEX idx_bike_station_status ON bike (current_station_id, bike_status);

CREATE TABLE dock (
    dock_id      BINARY(16)  NOT NULL,
    dock_status  SMALLINT    NOT NULL,
    station_id   BINARY(16)  NOT NULL,
    bike_id      BINARY(16)  NULL,
    CONSTRAINT pk_dock PRIMARY KEY (dock_id),
    CONSTRAINT ck_dock_status CHECK (dock_status BETWEEN 0 AND 2),
    CONSTRAINT ck_dock_bike_presence CHECK (
        (dock_status = 0 AND bike_id IS NULL) OR
        (dock_status = 1 AND bike_id IS NOT NULL) OR
        (dock_status = 2 AND bike_id IS NULL)
    ),
    CONSTRAINT uq_dock_bike UNIQUE (bike_id)
);

CREATE INDEX idx_dock_station_status ON dock (station_id, dock_status);

ALTER TABLE bike
    ADD CONSTRAINT fk_bike_station FOREIGN KEY (current_station_id)
        REFERENCES station (station_id) ON DELETE SET NULL;

ALTER TABLE dock
    ADD CONSTRAINT fk_dock_station FOREIGN KEY (station_id)
        REFERENCES station (station_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_dock_bike FOREIGN KEY (bike_id)
        REFERENCES bike (bike_id);

CREATE OR REPLACE VIEW station_summary AS
SELECT
    s.station_id,
    s.station_name,
    s.station_status,
    s.station_latitude,
    s.station_longtitude,
    s.address,
    s.station_capacity,
    COALESCE(SUM(CASE WHEN d.dock_status = 1 AND d.bike_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS bikes_docked,
    s.station_capacity - COALESCE(SUM(CASE WHEN d.dock_status = 1 AND d.bike_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS free_docks,
    COALESCE(SUM(CASE WHEN d.dock_status = 2 THEN 1 ELSE 0 END), 0) AS docks_out_of_service
FROM station s
LEFT JOIN dock d ON d.station_id = s.station_id
GROUP BY
    s.station_id,
    s.station_name,
    s.station_status,
    s.station_latitude,
    s.station_longtitude,
    s.address,
    s.station_capacity;
