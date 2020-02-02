package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderStatus;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
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
public class AllocateOrderAction implements Action<BeerOrderStatus, BeerOrderEvent> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatus, BeerOrderEvent> stateContext) {
        String idHeader = (String) stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER);

        if (idHeader != null) {
            BeerOrder beerOrder = beerOrderRepository.findOneById(UUID.fromString(idHeader));
            BeerOrderDto beerOrderDto = beerOrderMapper.beerOrderToDto(beerOrder);

            log.debug("Sending beerOrder id {} to allocate to {}", beerOrder.getId(), BeerOrderManagerImpl.BEER_ORDER_ID_HEADER);

            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_QUEUE, new AllocateOrderRequest(beerOrderDto));
        } else {
            throw new RuntimeException("Message without id header was sent " + stateContext.getMessage());
        }
    }
}
