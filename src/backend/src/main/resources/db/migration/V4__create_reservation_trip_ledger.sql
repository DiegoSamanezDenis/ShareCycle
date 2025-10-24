CREATE TABLE reservation (
    reservation_id        BINARY(16)  NOT NULL,
    rider_id              BINARY(16)  NOT NULL,
    station_id            BINARY(16)  NOT NULL,
    bike_id               BINARY(16)  NOT NULL,
    reserved_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_after_minutes INT         NOT NULL,
    active                TINYINT(1)  NOT NULL DEFAULT 1,
    expires_at            DATETIME    NOT NULL,
    CONSTRAINT pk_reservation PRIMARY KEY (reservation_id),
    CONSTRAINT ck_reservation_active CHECK (active IN (0, 1)),
    CONSTRAINT ck_reservation_duration CHECK (expires_after_minutes > 0),
    CONSTRAINT ck_reservation_expiration CHECK (expires_at > reserved_at)
);

CREATE INDEX idx_reservation_station_active ON reservation (station_id, active);
CREATE INDEX idx_reservation_expiry ON reservation (active, expires_at);
CREATE INDEX idx_reservation_bike_time ON reservation (bike_id, reserved_at DESC);
CREATE INDEX idx_reservation_rider_time ON reservation (rider_id, reserved_at DESC);

ALTER TABLE reservation
    ADD CONSTRAINT fk_reservation_rider FOREIGN KEY (rider_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_reservation_bike FOREIGN KEY (bike_id)
        REFERENCES bike (bike_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_reservation_station FOREIGN KEY (station_id)
        REFERENCES station (station_id) ON DELETE CASCADE;

CREATE TABLE trips (
    trip_id           BINARY(16)  NOT NULL,
    user_id           BINARY(16)  NOT NULL,
    bike_id           BINARY(16)  NOT NULL,
    start_station_id  BINARY(16)  NOT NULL,
    end_station_id    BINARY(16)  NULL,
    start_time        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time          DATETIME    NULL,
    duration_minutes  INT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_trips PRIMARY KEY (trip_id),
    CONSTRAINT ck_trips_end_after_start CHECK (end_time IS NULL OR end_time >= start_time),
    CONSTRAINT ck_trips_duration_nonneg CHECK (duration_minutes >= 0),
    CONSTRAINT ck_trips_requires_end_station CHECK (end_time IS NULL OR end_station_id IS NOT NULL)
);

CREATE INDEX idx_trips_user_time ON trips (user_id, start_time);
CREATE INDEX idx_trips_start_station ON trips (start_station_id);
CREATE INDEX idx_trips_end_station ON trips (end_station_id);
CREATE INDEX idx_trips_bike_time ON trips (bike_id, start_time);

ALTER TABLE trips
    ADD CONSTRAINT fk_trips_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_trips_bike FOREIGN KEY (bike_id)
        REFERENCES bike (bike_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_trips_start_station FOREIGN KEY (start_station_id)
        REFERENCES station (station_id),
    ADD CONSTRAINT fk_trips_end_station FOREIGN KEY (end_station_id)
        REFERENCES station (station_id);

CREATE TABLE ledger_entry (
    ledger_id    BINARY(16)    NOT NULL,
    user_id      BINARY(16)    NOT NULL,
    trip_id      BINARY(16)    NULL,
    status       SMALLINT      NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    timestamp    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ledger_entry PRIMARY KEY (ledger_id),
    CONSTRAINT ck_ledger_total_amount CHECK (total_amount >= 0)
);

CREATE UNIQUE INDEX uq_ledger_trip ON ledger_entry (trip_id);
CREATE INDEX idx_ledger_user_time ON ledger_entry (user_id, timestamp DESC);

ALTER TABLE ledger_entry
    ADD CONSTRAINT fk_ledger_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ledger_trip FOREIGN KEY (trip_id)
        REFERENCES trips (trip_id) ON DELETE SET NULL;
