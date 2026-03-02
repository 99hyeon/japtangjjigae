package com.example.japtangjjigae.train.repository;

import com.example.japtangjjigae.train.dto.TrainRunSearchRow;
import com.example.japtangjjigae.train.entity.TrainRun;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainRunRepository extends JpaRepository<TrainRun, Long> {

    @Query(
        value = """
              select TrainRunSearchRow(
                tr.id,
                t.id,
                t.trainCode,
                s1.departureAt,
                s2.arrivalAt,
                s1.stopOrder,
                s2.stopOrder,
                s1.cumulativeFare,
                s2.cumulativeFare
              )
              from TrainStop s1
              join TrainStop s2 on s1.trainRun = s2.trainRun
              join s1.trainRun tr
              join tr.train t
              where s1.station.code = :originStationCode
                and s2.station.code = :destinationStationCode
                and tr.runDate = :runDate
                and s1.stopOrder < s2.stopOrder
                and s1.departureAt >= :departureTime
              order by s1.departureAt
            """,
        countQuery = """
              select count(distinct tr)
              from TrainStop s1
              join TrainStop s2 on s1.trainRun = s2.trainRun
              join s1.trainRun tr
              where s1.station.code = :originStationCode
                and s2.station.code = :destinationStationCode
                and tr.runDate = :runDate
                and s1.stopOrder < s2.stopOrder
                and s1.departureAt >= :departureTime
            """
    )
    Page<TrainRunSearchRow> findTrainRunRows(
        @Param("originStationCode") String originStationCode,
        @Param("destinationStationCode") String destinationStationCode,
        @Param("runDate") LocalDate runDate,
        @Param("departureTime") LocalTime departureTime,
        Pageable pageable
    );

}
