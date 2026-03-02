package com.example.japtangjjigae.train.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

import com.example.japtangjjigae.order.Order;
import com.example.japtangjjigae.redis.seathold.SeatHoldStore;
import com.example.japtangjjigae.redis.seathold.SeatHoldStore.SeatHold;
import com.example.japtangjjigae.station.repository.StationRepository;
import com.example.japtangjjigae.ticket.PayStatus;
import com.example.japtangjjigae.ticket.entity.Ticket;
import com.example.japtangjjigae.ticket.repository.TicketRepository;
import com.example.japtangjjigae.train.dto.TrainInfoDTO;
import com.example.japtangjjigae.train.dto.TrainSearchRequestDTO;
import com.example.japtangjjigae.train.dto.TrainSearchResponseDTO;
import com.example.japtangjjigae.train.entity.Carriage;
import com.example.japtangjjigae.train.entity.Seat;
import com.example.japtangjjigae.train.entity.Train;
import com.example.japtangjjigae.train.entity.TrainRun;
import com.example.japtangjjigae.train.entity.TrainStop;
import com.example.japtangjjigae.train.repository.CarriageRepository;
import com.example.japtangjjigae.train.repository.SeatRepository;
import com.example.japtangjjigae.train.repository.TrainRunRepository;
import com.example.japtangjjigae.train.repository.TrainStopRepository;
import com.example.japtangjjigae.user.common.OAuthProvider;
import com.example.japtangjjigae.user.entity.User;
import com.example.japtangjjigae.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrainServiceTest {

    private static final String SEOUL = "SEOUL";
    private static final String SUWON = "SUWON";
    private static final String DAEJEON = "DAEJEON";
    private static final String BUSAN = "BUSAN";

    @Autowired
    private TrainService trainService;
    @Autowired
    private TrainRunRepository trainRunRepository;
    @Autowired
    private TrainStopRepository trainStopRepository;
    @Autowired
    private CarriageRepository carriageRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private StationRepository stationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired private EntityManager em;

    @MockitoBean
    private SeatHoldStore seatHoldStore;

    @BeforeEach
    void setUp() {
        // 기본은 홀드 없음
        given(seatHoldStore.findOverLappingHolds(anyLong(), anyInt(), anyInt()))
            .willReturn(List.of());
    }

    @Test
    @DisplayName("searchTrain - 구간겹침 예약 내역으로 soldout")
    void searchTrain_soldOut_byOverlappingTickets() {
        //given
        LocalDate runDate = LocalDate.now().plusDays(1);
        TrainRun gb101 = getTrainRun(runDate, "GB101");

        TrainSearchRequestDTO req = new TrainSearchRequestDTO(SEOUL, DAEJEON, runDate, LocalTime.of(0, 0), 1);

        TrainStop ticketDep = getStop(gb101, SUWON);
        TrainStop ticketArr = getStop(gb101, BUSAN);

        User user = persistTestUser();
        bookAllSeatsWithOrder(user, gb101, ticketDep, ticketArr);

        TrainSearchResponseDTO res = trainService.searchTrain(req, 0);

        // when & then
        TrainInfoDTO gb101Info = findTrain(res, "GB101");
        assertThat(gb101Info.isSoldOut()).isTrue();

        TrainInfoDTO gb103Info = findTrain(res, "GB103");
        assertThat(gb103Info.isSoldOut()).isFalse();
    }


    @Test
    @DisplayName("searchTrain - 구간겹침 홀드 내역으로 soldout")
    void searchTrain_soldOut_byOverlappingHolds() {
        //given
        LocalDate runDate = LocalDate.now().plusDays(1);
        TrainRun gb101 = getTrainRun(runDate, "GB101");

        TrainSearchRequestDTO req = new TrainSearchRequestDTO(SEOUL, DAEJEON, runDate, LocalTime.of(0, 0), 1);

        int holdDepOrder = getStop(gb101, SUWON).getStopOrder();
        int holdArrOrder = getStop(gb101, BUSAN).getStopOrder();

        List<Long> seatIds = getAllSeatIds(gb101.getTrain());
        List<HoldMeta> stored = seatIds.stream()
            .map(seatId -> new HoldMeta(gb101.getId(), seatId, holdDepOrder, holdArrOrder))
            .toList();

        stubSeatHoldStoreLikeRealOverlapFiltering(stored);

        TrainSearchResponseDTO res = trainService.searchTrain(req, 0);

        // when & then
        TrainInfoDTO gb101Info = findTrain(res, "GB101");
        assertThat(gb101Info.isSoldOut()).isTrue();
    }

    @Test
    @DisplayName("searchTrain - 경계선으로 안겹치는 예약 내역과 함께 조회 성공")
    void searchTrain_success_whenTicketTouchesBoundary() {
        //given
        LocalDate runDate = LocalDate.now().plusDays(1);
        TrainRun gb101 = getTrainRun(runDate, "GB101");

        TrainSearchRequestDTO req = new TrainSearchRequestDTO(SEOUL, SUWON, runDate, LocalTime.of(0, 0), 1);

        TrainStop ticketDep = getStop(gb101, SUWON);
        TrainStop ticketArr = getStop(gb101, DAEJEON);

        User user = persistTestUser();
        bookAllSeatsWithOrder(user, gb101, ticketDep, ticketArr);

        TrainSearchResponseDTO res = trainService.searchTrain(req, 0);

        // when & then
        TrainInfoDTO gb101Info = findTrain(res, "GB101");
        assertThat(gb101Info.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("searchTrain - 경계선으로 안겹치는 홀드 내역과 함께 조회 성공")
    void searchTrain_success_whenHoldTouchesBoundary() {
        //given
        LocalDate runDate = LocalDate.now().plusDays(1);
        TrainRun gb101 = getTrainRun(runDate, "GB101");

        TrainSearchRequestDTO req = new TrainSearchRequestDTO(SEOUL, SUWON, runDate, LocalTime.of(0, 0), 1);

        int holdDepOrder = getStop(gb101, SUWON).getStopOrder();   // 2
        int holdArrOrder = getStop(gb101, DAEJEON).getStopOrder(); // 3

        List<Long> seatIds = getAllSeatIds(gb101.getTrain());
        List<HoldMeta> stored = seatIds.stream()
            .map(seatId -> new HoldMeta(gb101.getId(), seatId, holdDepOrder, holdArrOrder))
            .toList();

        stubSeatHoldStoreLikeRealOverlapFiltering(stored);

        TrainSearchResponseDTO res = trainService.searchTrain(req, 0);

        // when & then
        TrainInfoDTO gb101Info = findTrain(res, "GB101");
        assertThat(gb101Info.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("searchTrain - 조건에 해당되는 기차 없는 경우")
    void searchTrain_empty_whenNoMatchingTrains() {
        //given
        LocalDate runDateWithNoData = LocalDate.now().plusDays(30);

        TrainSearchRequestDTO req = new TrainSearchRequestDTO(SEOUL, BUSAN, runDateWithNoData, LocalTime.of(0, 0), 1);

        TrainSearchResponseDTO res = trainService.searchTrain(req, 0);

        // when & then
        assertThat(res.getTrains()).isEmpty();
    }


    private void bookAllSeatsWithOrder(User user, TrainRun run, TrainStop dep, TrainStop arr) {
        List<Seat> seats = getAllSeats(run.getTrain());
        int amount = arr.getCumulativeFare() - dep.getCumulativeFare();

        List<Ticket> tickets = new ArrayList<>(seats.size());
        for (Seat seat : seats) {
            Ticket t = Ticket.createTicket(run, dep, arr, seat, amount);
            tickets.add(t);
        }

        Order order = Order.createOrder(user, tickets, PayStatus.COMPLETE);

        em.persist(order);
        em.flush();

        ticketRepository.flush();
    }

    private User persistTestUser() {
        String socialId = "sid-" + UUID.randomUUID();
        String phone = "010" + (int)(Math.random() * 1_0000_0000);

        User u = User.createUser(socialId, OAuthProvider.KAKAO, "tester", phone);
        userRepository.save(u);
        userRepository.flush();
        return u;
    }

    // -------------------------
    // SeatHoldStore 스텁(겹침 필터링처럼)
    // -------------------------
    private void stubSeatHoldStoreLikeRealOverlapFiltering(List<HoldMeta> stored) {
        willAnswer(inv -> {
            long trainRunId = inv.getArgument(0);
            int reqDep = inv.getArgument(1);
            int reqArr = inv.getArgument(2);

            // 겹침 조건: holdDep < reqArr && reqDep < holdArr
            return stored.stream()
                .filter(h -> h.trainRunId == trainRunId)
                .filter(h -> h.departureOrder < reqArr && reqDep < h.arrivalOrder)
                .map(h -> mockSeatHold(h.seatId))
                .toList();
        }).given(seatHoldStore).findOverLappingHolds(anyLong(), anyInt(), anyInt());
    }

    private SeatHold mockSeatHold(long seatId) {
        SeatHold hold = mock(SeatHold.class);
        given(hold.seatId()).willReturn(seatId);
        return hold;
    }

    // -------------------------
    // 초기데이터 조회 헬퍼
    // -------------------------
    private TrainRun getTrainRun(LocalDate runDate, String trainCode) {
        return trainRunRepository.findAll().stream()
            .filter(tr -> tr.getRunDate().equals(runDate))
            .filter(tr -> tr.getTrain() != null && trainCode.equals(tr.getTrain().getTrainCode()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("초기 데이터에서 " + trainCode + " TrainRun을 찾지 못함"));
    }

    private TrainStop getStop(TrainRun run, String stationCode) {
        return trainStopRepository.findByTrainRunAndStation_Code(run, stationCode)
            .orElseThrow(() -> new IllegalStateException("TrainStop 없음: " + stationCode));
    }

    private List<Seat> getAllSeats(Train train) {
        List<Carriage> carriages = carriageRepository.findByTrainOrderByCarriageNumberAsc(train);
        return seatRepository.findByCarriageInOrderByCarriage_IdAscRowNumberAscColumnCodeAsc(carriages);
    }

    private List<Long> getAllSeatIds(Train train) {
        return getAllSeats(train).stream().map(Seat::getId).toList();
    }

    private TrainInfoDTO findTrain(TrainSearchResponseDTO res, String trainCode) {
        return res.getTrains().stream()
            .filter(t -> trainCode.equals(t.getTrainCode()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("응답에서 " + trainCode + " 열차를 찾지 못함"));
    }


    private static class HoldMeta {
        final long trainRunId;
        final long seatId;
        final int departureOrder;
        final int arrivalOrder;

        HoldMeta(long trainRunId, long seatId, int departureOrder, int arrivalOrder) {
            this.trainRunId = trainRunId;
            this.seatId = seatId;
            this.departureOrder = departureOrder;
            this.arrivalOrder = arrivalOrder;
        }
    }

}