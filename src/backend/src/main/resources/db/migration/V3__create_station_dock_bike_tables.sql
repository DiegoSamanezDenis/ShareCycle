CREATE TABLE bike
(
    bike_id BINARY (16) NOT NULL,
    bike_type          SMALLINT NOT NULL,
    bike_status        SMALLINT NOT NULL,
    reservation_expiry datetime NULL,
    CONSTRAINT pk_bike PRIMARY KEY (bike_id)
);

CREATE TABLE dock
(
    dock_id BINARY (16) NOT NULL,
    dock_status SMALLINT NOT NULL,
    station_id BINARY (16) NULL,
    bike_id BINARY (16) NULL,
    CONSTRAINT pk_dock PRIMARY KEY (dock_id)
);

CREATE TABLE station
(
    station_id BINARY (16) NOT NULL,
    station_name       VARCHAR(255) NULL,
    station_status     SMALLINT     NOT NULL,
    station_latitude   DOUBLE       NOT NULL,
    station_longtitude DOUBLE       NOT NULL,
    bikes_docked       INT          NOT NULL,
    station_capacity   INT          NOT NULL,
    address            VARCHAR(255) NOT NULL,
    CONSTRAINT pk_station PRIMARY KEY (station_id)
);

ALTER TABLE dock
    ADD CONSTRAINT uc_dock_bike UNIQUE (bike_id);

ALTER TABLE dock
    ADD CONSTRAINT FK_DOCK_ON_BIKE FOREIGN KEY (bike_id) REFERENCES bike (bike_id);

ALTER TABLE dock
    ADD CONSTRAINT FK_DOCK_ON_STATION FOREIGN KEY (station_id) REFERENCES station (station_id);