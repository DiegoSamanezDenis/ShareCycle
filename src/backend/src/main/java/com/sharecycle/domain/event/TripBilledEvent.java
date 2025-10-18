package com.sharecycle.domain.event;

import java.util.UUID;

public record TripBilledEvent (
        UUID tripId,
        UUID ledgerId
){
}
