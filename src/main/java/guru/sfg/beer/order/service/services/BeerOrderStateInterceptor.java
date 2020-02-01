package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderStatus;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderStateInterceptor extends StateMachineInterceptorAdapter<BeerOrderStatus, BeerOrderEvent> {

    private final BeerOrderRepository beerOrderRepository;

    @Override
    public void preStateChange(State<BeerOrderStatus, BeerOrderEvent> state, Message<BeerOrderEvent> message,
                               Transition<BeerOrderStatus, BeerOrderEvent> transition,
                               StateMachine<BeerOrderStatus, BeerOrderEvent> stateMachine) {
        if (message != null) {
            Optional.ofNullable((String) message.getHeaders().get(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER))
                    .ifPresent(idHeader -> {
                        UUID beerOrderId = UUID.fromString(idHeader);
                        log.debug("Saving state for order id: " + beerOrderId + " Status: " + state.getId());

                        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);

                        beerOrder.setOrderStatus(state.getId());

                        beerOrderRepository.saveAndFlush(beerOrder);
                    });
        }
    }
}
