package com.sharecycle.domain.event;

import com.sharecycle.domain.model.Bill;

import java.util.UUID;

public record PaymentSucceedEvent(UUID riderId, UUID tripId){
}
