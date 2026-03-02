package com.example.japtangjjigae.ticket.repository;

import com.example.japtangjjigae.ticket.entity.Ticket;
import com.example.japtangjjigae.train.entity.TrainRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
          select count(distinct t.seat.id)
          from Ticket t
            join t.departureStop ds
            join t.arrivalStop arr
          where t.trainRun.id = :trainRunId
            and ds.stopOrder < :requestArrivalOrder
            and :requestDepartureOrder < arr.stopOrder
            and t.deletedAt is null
        """)
    int countBookedSeatsInSection(
        @Param("trainRunId") Long trainRunId,
        @Param("requestDepartureOrder") int requestDepartureOrder,
        @Param("requestArrivalOrder") int requestArrivalOrder
    );

    @Query("""
            select distinct t.seat.id
            from Ticket t
                join t.departureStop ds
                join t.arrivalStop arr
            where t.trainRun = :trainRun
              and ds.stopOrder < :requestArrivalOrder
              and :requestDepartureOrder < arr.stopOrder
              and t.deletedAt is null
        """)
    List<Long> findBookedSeatIdsInSection(
        @Param("trainRun") TrainRun trainRun,
        @Param("requestDepartureOrder") int requestDepartureOrder,
        @Param("requestArrivalOrder") int requestArrivalOrder
    );
}
