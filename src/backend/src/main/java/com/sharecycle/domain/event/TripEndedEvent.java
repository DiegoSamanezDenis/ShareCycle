package com.sharecycle.domain.event;

import com.sharecycle.model.entity.*;

import java.time.LocalDateTime;
import java.util.UUID;

public record TripEndedEvent (
        UUID tripId
){

}
