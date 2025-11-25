package com.sharecycle.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;

public class TripBuilder {
    private UUID tripId;
    private Rider rider;
    private Station startStation;
    private Bike bike;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Station endStation;
    private double appliedDiscountRate = 0.0;

    public TripBuilder() {
    }

   public TripBuilder(Trip trip) {
        tripId = trip.getTripID();
        rider = trip.getRider();
        startStation = trip.getStartStation();
        bike = trip.getBike();
        startTime = trip.getStartTime();
        endTime = trip.getEndTime();
        endStation = trip.getEndStation();
        appliedDiscountRate = trip.getAppliedDiscountRate();
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
        Trip t = new Trip(getTripId(),getStartTime(),getEndTime(),getRider(), getBike(),getStartStation(),getEndStation());
        t.setAppliedDiscountRate(this.appliedDiscountRate);
        return t;
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

    public UUID getTripId() {
        return tripId;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }

    public Station getEndStation() {
        return endStation;
    }

    public void setEndStation(Station endStation) {
        this.endStation = endStation;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Bike getBike() {
        return bike;
    }

    public void setBike(Bike bike) {
        this.bike = bike;
    }

    public Station getStartStation() {
        return startStation;
    }

    public void setStartStation(Station startStation) {
        this.startStation = startStation;
    }

    public Rider getRider() {
        return rider;
    }

    public void setRider(Rider rider) {
        this.rider = rider;
    }

    public double getAppliedDiscountRate() {
        return appliedDiscountRate;
    }

    public void setAppliedDiscountRate(double appliedDiscountRate) {
        this.appliedDiscountRate = appliedDiscountRate;
    }
}
