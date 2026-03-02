package com.example.japtangjjigae.train.dto;

import java.time.LocalTime;

public record TrainRunSearchRow (
    Long trainRunId,
    Long trainId,
    String trainCode,
    LocalTime departureAt,
    LocalTime arrivalAt,
    int departureOrder,
    int arrivalOrder,
    int departureFare,
    int arrivalFare
) {}
