CREATE TABLE ledger_entry
(
    ledger_id BINARY (16) NOT NULL,
    user_id BINARY (16) NULL,
    trip_id BINARY (16) NULL,
    status       SMALLINT NOT NULL,
    total_amount DOUBLE   NOT NULL,
    timestamp    datetime NOT NULL,
    CONSTRAINT pk_ledgerentry PRIMARY KEY (ledger_id)
);

CREATE TABLE reservation
(
    reservation_id BINARY (16) NOT NULL,
    rider_id BINARY (16) NULL,
    station_id BINARY (16) NULL,
    bike_id BINARY (16) NULL,
    reserved_at           datetime NULL,
    expires_at            datetime NULL,
    expires_after_minutes INT      NOT NULL,
    active                BIT(1)   NOT NULL,
    CONSTRAINT pk_reservation PRIMARY KEY (reservation_id)
);

CREATE TABLE trips
(
    trip_id BINARY (16) NOT NULL,
    start_time       timestamp NULL,
    end_time         timestamp NULL,
    duration_minutes INT       NOT NULL,
    user_id BINARY (16) NULL,
    bike_id BINARY (16) NULL,
    start_station_id BINARY (16) NOT NULL,
    end_station_id BINARY (16) NOT NULL,
    CONSTRAINT pk_trips PRIMARY KEY (trip_id)
);

ALTER TABLE ledger_entry
    ADD CONSTRAINT uc_ledgerentry_trip UNIQUE (trip_id);

ALTER TABLE trips
    ADD CONSTRAINT uc_trips_bike UNIQUE (bike_id);

ALTER TABLE trips
    ADD CONSTRAINT uc_trips_user UNIQUE (user_id);

ALTER TABLE ledger_entry
    ADD CONSTRAINT FK_LEDGERENTRY_ON_TRIP FOREIGN KEY (trip_id) REFERENCES trips (trip_id);

ALTER TABLE ledger_entry
    ADD CONSTRAINT FK_LEDGERENTRY_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE reservation
    ADD CONSTRAINT FK_RESERVATION_ON_BIKE FOREIGN KEY (bike_id) REFERENCES bike (bike_id);

ALTER TABLE reservation
    ADD CONSTRAINT FK_RESERVATION_ON_RIDER FOREIGN KEY (rider_id) REFERENCES users (user_id);

ALTER TABLE reservation
    ADD CONSTRAINT FK_RESERVATION_ON_STATION FOREIGN KEY (station_id) REFERENCES station (station_id);

ALTER TABLE trips
    ADD CONSTRAINT FK_TRIPS_ON_BIKE FOREIGN KEY (bike_id) REFERENCES bike (bike_id);

ALTER TABLE trips
    ADD CONSTRAINT FK_TRIPS_ON_END_STATION FOREIGN KEY (end_station_id) REFERENCES station (station_id);

ALTER TABLE trips
    ADD CONSTRAINT FK_TRIPS_ON_START_STATION FOREIGN KEY (start_station_id) REFERENCES station (station_id);

ALTER TABLE trips
    ADD CONSTRAINT FK_TRIPS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id);