package com.sharecycle.application;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import com.sharecycle.infrastructure.persistence.JpaTripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ListTripsUseCase {

    private final Logger logger = LoggerFactory.getLogger(ListTripsUseCase.class);

    private final JpaTripRepository jpaTripRepository;

    public ListTripsUseCase(JpaTripRepository jpaTripRepository) {
        this.jpaTripRepository = jpaTripRepository;
    }


    public List<Trip> execute(User user, LocalDateTime startTime, LocalDateTime endTime, Bike.BikeType bikeType) {
        String role = user.getRole();
        if (role.equals("OPERATOR")){
            logger.info("User is an operator, finding all trips");
            return jpaTripRepository.findAllWithFilter(startTime, endTime, bikeType);
        } else {
            logger.info("User is a rider, finding all trips by this rider");
            return jpaTripRepository.findAllByUserIdWithFilter(user.getUserId(), startTime, endTime, bikeType);
        }
    }
}
