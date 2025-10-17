package com.sharecycle.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sharecycle.model.entity.Bike;
import com.sharecycle.model.entity.Rider;
import com.sharecycle.model.entity.Station;
import com.sharecycle.model.entity.Trip;

public class TripBuilder {
    private Rider rider;
    private Station startStation;
    private Bike bike;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Station endStation;

    public TripBuilder() {

    }
    public void start(Rider rider, Station startStation, Bike bike, LocalDateTime startTime) {
        this.rider = rider;
        this.startStation = startStation;
        this.bike = bike;
        this.startTime = startTime;
    }
    public void endAt(Station s, LocalDateTime at){
        setEndTime(at);
        setEndStation(s);
    }
    public Trip build()
    {
        return new Trip(UUID.randomUUID(),getStartTime(),getEndTime(),getRider(), getBike(),getStartStation(),getEndStation());
    }

    @Override
    public String toString() {
        return "TripBuilder{" +
                "rider=" + rider +
                ", startStation=" + startStation +
                ", bike=" + bike +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", endStation=" + endStation +
                '}';
    }

    public Rider getRider() {
        return rider;
    }

    public Station getStartStation() {
        return startStation;
    }

    public Bike getBike() {
        return bike;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Station getEndStation() {
        return endStation;
    }

    public void setEndStation(Station endStation) {
        this.endStation = endStation;
    }
}
