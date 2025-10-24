CREATE TABLE domain_event (
    event_id          BINARY(16)   NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    occurred_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message           VARCHAR(500) NULL,
    payload           JSON         NULL,
    principal_user_id BINARY(16)   NULL,
    station_id        BINARY(16)   NULL,
    bike_id           BINARY(16)   NULL,
    trip_id           BINARY(16)   NULL,
    CONSTRAINT pk_domain_event PRIMARY KEY (event_id)
);

CREATE INDEX idx_domain_event_occurred ON domain_event (occurred_at DESC);
CREATE INDEX idx_domain_event_type ON domain_event (event_type);

ALTER TABLE domain_event
    ADD CONSTRAINT fk_event_user FOREIGN KEY (principal_user_id)
        REFERENCES users (user_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_event_station FOREIGN KEY (station_id)
        REFERENCES station (station_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_event_bike FOREIGN KEY (bike_id)
        REFERENCES bike (bike_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_event_trip FOREIGN KEY (trip_id)
        REFERENCES trips (trip_id) ON DELETE SET NULL;
