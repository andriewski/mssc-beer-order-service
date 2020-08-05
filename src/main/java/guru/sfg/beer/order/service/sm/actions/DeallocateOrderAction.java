package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderStatus;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.events.DeallocateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.ObjectNotFoundException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeallocateOrderAction implements Action<BeerOrderStatus, BeerOrderEvent> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderMapper beerOrderMapper;
    private final BeerOrderRepository beerOrderRepository;

    @Override
    public void execute(StateContext<BeerOrderStatus, BeerOrderEvent> stateContext) {
        Object idHeader = stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER);
        log.error("Compensating Transaction... Deallocate order id: {}", idHeader);

        if (idHeader != null) {
            BeerOrder beerOrder = beerOrderRepository.findById(UUID.fromString(idHeader.toString()))
                    .orElseThrow(() -> {
                        throw new ObjectNotFoundException(idHeader.toString(), "beerOrder");
                    });

            jmsTemplate.convertAndSend(
                    JmsConfig.DEALLOCATE_ORDER_QUEUE,
                    DeallocateOrderRequest.builder()
                            .beerOrderDto(beerOrderMapper.toBeerDto(beerOrder))
                            .build()
            );
        }
    }
}
