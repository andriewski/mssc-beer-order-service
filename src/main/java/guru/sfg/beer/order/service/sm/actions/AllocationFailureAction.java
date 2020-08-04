package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderStatus;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.brewery.model.events.AllocationFailureEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationFailureAction implements Action<BeerOrderStatus, BeerOrderEvent> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatus, BeerOrderEvent> stateContext) {
        Object idHeader = stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER);
        log.error("Compensating Transaction... Allocation failed for order id: {}", idHeader);

        if (idHeader != null) {
            jmsTemplate.convertAndSend(
                    JmsConfig.FAILURE_ALLOCATION_QUEUE,
                    AllocationFailureEvent.builder()
                            .orderId(UUID.fromString(idHeader.toString()))
                            .build()
            );
        }
    }
}
