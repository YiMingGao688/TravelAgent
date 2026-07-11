package com.itheima.travelagent2.modules.agenttrip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.agenttrip.dto.CardConfirmResponseDTO;
import com.itheima.travelagent2.modules.agenttrip.entity.AgentRun;
import com.itheima.travelagent2.modules.agenttrip.entity.FinalItineraryItem;
import com.itheima.travelagent2.modules.agenttrip.entity.TripPlanCard;
import com.itheima.travelagent2.modules.agenttrip.mapper.AgentRunMapper;
import com.itheima.travelagent2.modules.agenttrip.mapper.FinalItineraryItemMapper;
import com.itheima.travelagent2.modules.agenttrip.mapper.TripPlanCardMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripCardConfirmService {
    private final TripPlanCardMapper cardMapper;
    private final FinalItineraryItemMapper finalItemMapper;
    private final AgentRunMapper runMapper;
    private final AgentEventPublisherService eventPublisher;

    public TripCardConfirmService(TripPlanCardMapper cardMapper,
                                  FinalItineraryItemMapper finalItemMapper,
                                  AgentRunMapper runMapper,
                                  AgentEventPublisherService eventPublisher) {
        this.cardMapper = cardMapper;
        this.finalItemMapper = finalItemMapper;
        this.runMapper = runMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CardConfirmResponseDTO confirm(Long cardId) {
        TripPlanCard card = cardMapper.selectById(cardId);
        if (card == null) {
            throw new IllegalArgumentException("未找到行程卡片：" + cardId);
        }
        if (!Boolean.TRUE.equals(card.getConfirmed())) {
            card.setConfirmed(true);
            card.setStatus("CONFIRMED");
            cardMapper.updateById(card);
        }
        FinalItineraryItem existing = finalItemMapper.selectOne(new LambdaQueryWrapper<FinalItineraryItem>()
                .eq(FinalItineraryItem::getCardId, cardId)
                .last("LIMIT 1"));
        if (existing == null) {
            FinalItineraryItem item = new FinalItineraryItem();
            item.setRunId(card.getRunId());
            item.setCardId(card.getId());
            item.setTitle(card.getTitle());
            item.setContent(card.getContent());
            item.setLocation(card.getSubtitle());
            item.setStatus("CONFIRMED");
            finalItemMapper.insert(item);
        }
        updateRunIfReady(card.getRunId());
        CardConfirmResponseDTO response = new CardConfirmResponseDTO();
        response.setCard(cardMapper.selectById(cardId));
        response.setFinalItinerary(listFinal(card.getRunId()));
        return response;
    }

    public List<FinalItineraryItem> listFinal(Long runId) {
        return finalItemMapper.selectList(new LambdaQueryWrapper<FinalItineraryItem>()
                .eq(FinalItineraryItem::getRunId, runId)
                .orderByAsc(FinalItineraryItem::getCreatedAt)
                .orderByAsc(FinalItineraryItem::getId));
    }

    private void updateRunIfReady(Long runId) {
        Long pendingCount = cardMapper.selectCount(new LambdaQueryWrapper<TripPlanCard>()
                .eq(TripPlanCard::getRunId, runId)
                .eq(TripPlanCard::getConfirmRequired, true)
                .eq(TripPlanCard::getConfirmed, false));
        if (pendingCount == 0) {
            AgentRun run = runMapper.selectById(runId);
            if (run != null) {
                run.setStatus("DONE");
                run.setCurrentStage(AgentTripStage.FINAL_ITINERARY_GENERATION);
                runMapper.updateById(run);
            }
            eventPublisher.publish(null,
                    runId,
                    AgentTripStage.FINAL_ITINERARY_GENERATION,
                    "生成最终行程单",
                    "所有需要确认的卡片已加入最终行程单。",
                    "DONE",
                    java.util.Map.of("runId", runId),
                    90);
        }
    }
}
