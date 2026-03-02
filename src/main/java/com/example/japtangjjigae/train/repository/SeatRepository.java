package com.example.japtangjjigae.train.repository;

import com.example.japtangjjigae.train.entity.Carriage;
import com.example.japtangjjigae.train.entity.Seat;
import com.example.japtangjjigae.train.entity.Train;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByCarriageOrderByRowNumberAscColumnCodeAsc(Carriage carriage);
    List<Seat> findByCarriageInOrderByCarriage_IdAscRowNumberAscColumnCodeAsc(List<Carriage> carriages);

    int countByCarriage_Train(Train train);
    int countByCarriage_Train_Id(Long trainId);
    List<Seat> findByCarriage_Train(Train train);
}
