package com.example.japtangjjigae.redis.seathold;

import com.example.japtangjjigae.redis.AbstractRedisStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisSeatHoldStore extends AbstractRedisStore implements SeatHoldStore {

    //구간별 인덱스 key
    private static final String INDEX_PREFIX = "seat-hold-index:";

    //구간 겹침 위한 key
    private static final String SEG_PREFIX = "seat-hold-orders:";

    /**
     * 1. 이미 홀드된 구간 있으면 즉시 실패
     * 2. 전부 비어있으면 세그먼트 키를 전부 SET + TTL
     * 3. KEYS[i] 문자열을 파싱해서 인덱스 Set에도 seatId를 넣음
     */
    private final DefaultRedisScript<Long> holdMultiScript2 = new DefaultRedisScript<>("""
        for i=1,#KEYS do
          if redis.call('EXISTS', KEYS[i]) == 1 then
            return 0
          end
        end

        for i=1,#KEYS do
          redis.call('SET', KEYS[i], ARGV[1], 'EX', ARGV[2])

          -- KEYS[i] = seat-hold-orders:{trainRunId}:{seatId}:{order}
          local parts = {}
          for part in string.gmatch(KEYS[i], "([^:]+)") do
            table.insert(parts, part)
          end

          local trainRunId = parts[2]
          local seatId = parts[3]
          local order = parts[4]

          local indexKey = "seat-hold-index:" .. trainRunId .. ":" .. order
          redis.call('SADD', indexKey, seatId)
        end

        return 1
        """, Long.class);

    public RedisSeatHoldStore(StringRedisTemplate stringRedisTemplate) {
        super(stringRedisTemplate);
    }

    @Override
    public boolean holdSeat(Long userId, Long trainRunId, List<Long> seatIds, int depOrder, int arrOrder,
        long ttlSeconds) {
        List<String> keys = new ArrayList<>();
        for (Long seatId : seatIds) {
            for (int order = depOrder; order < arrOrder; order++) {
                keys.add(SEG_PREFIX + trainRunId + ":" + seatId + ":" + order);
            }
        }

        Long result = stringRedisTemplate.execute(holdMultiScript2, keys, String.valueOf(userId),
            String.valueOf(ttlSeconds));
        return result != null && result == 1L;
    }

    @Override
    public List<SeatHold> findOverLappingHolds(Long trainRunId, int requestDepartureOrder,
        int requestArrivalOrder) {
        Set<Long> overlappedSeatIds = new HashSet<>();

        for (int order = requestDepartureOrder; order < requestArrivalOrder; order++) {
            int currentOrder = order;
            String indexKey = INDEX_PREFIX + trainRunId + ":" + currentOrder;

            Set<String> seatIdStrs = getSetMembers(indexKey);
            if (seatIdStrs == null || seatIdStrs.isEmpty()) continue;

            List<String> seatIdList = new ArrayList<>(seatIdStrs);
            List<String> segKeys = seatIdList.stream()
                .map(seatIdStr -> SEG_PREFIX + trainRunId + ":" + seatIdStr + ":" + currentOrder)
                .toList();

            List<String> values = stringRedisTemplate.opsForValue().multiGet(segKeys);

            List<String> expiredToRemove = new ArrayList<>();
            for (int i = 0; i < seatIdList.size(); i++) {
                String seatIdStr = seatIdList.get(i);
                String v = (values != null ? values.get(i) : null);

                if (v == null) {
                    expiredToRemove.add(seatIdStr);
                } else {
                    overlappedSeatIds.add(Long.valueOf(seatIdStr));
                }
            }

            if (!expiredToRemove.isEmpty()) {
                stringRedisTemplate.opsForSet().remove(indexKey, expiredToRemove.toArray());
            }
        }

        List<SeatHold> result = new ArrayList<>();
        for (Long seatId : overlappedSeatIds) {
            result.add(new SeatHold(seatId, requestDepartureOrder, requestArrivalOrder));
        }
        return result;
    }

    @Override
    public void releaseSeat(Long trainRunId, List<Long> seatIds, int depOrder, int arrOrder) {
        List<String> segKeys = new ArrayList<>();
        for (Long seatId : seatIds) {
            for (int order = depOrder; order < arrOrder; order++) {
                segKeys.add(SEG_PREFIX + trainRunId + ":" + seatId + ":" + order);
                String indexKey = INDEX_PREFIX + trainRunId + ":" + order;
                stringRedisTemplate.opsForSet().remove(indexKey, String.valueOf(seatId));
            }
        }
        stringRedisTemplate.delete(segKeys);
    }
}
