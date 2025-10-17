ALTER TABLE bike
    ADD COLUMN current_station_id BINARY(16) NULL;

ALTER TABLE bike
    ADD CONSTRAINT FK_BIKE_ON_STATION
        FOREIGN KEY (current_station_id) REFERENCES station (station_id);
