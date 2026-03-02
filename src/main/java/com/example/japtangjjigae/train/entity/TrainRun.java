package com.example.japtangjjigae.train.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(nullable = false)
    private LocalDate runDate;

    public static TrainRun createTrainRun(Train train, LocalDate runDate) {
        TrainRun newTrainRun = new TrainRun();
        newTrainRun.train = train;
        newTrainRun.runDate = runDate;

        return newTrainRun;
    }

    public Long getId() {
        return id;
    }

    public Train getTrain() {
        return train;
    }

    public LocalDate getRunDate(){
        return runDate;
    }
}
