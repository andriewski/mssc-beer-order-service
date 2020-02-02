package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderStatus;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    private final StateMachineFactory<BeerOrderStatus, BeerOrderEvent> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateInterceptor beerOrderStateInterceptor;

    public static final String BEER_ORDER_ID_HEADER = "beer_order_id";

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatus.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);

        sendBeerOrderEvent(savedBeerOrder, BeerOrderEvent.VALIDATE_ORDER);

        return savedBeerOrder;
    }

    @Override
    public void processValidationResult(UUID beerOrderId, boolean isValid) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);

        if (isValid) {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_PASSED);

            BeerOrder validatedBeerOrder = beerOrderRepository.findOneById(beerOrderId);

            sendBeerOrderEvent(validatedBeerOrder, BeerOrderEvent.ALLOCATE_ORDER);
        } else {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_FAILED);
        }
    }

    @Override
    public void processAllocationResult(UUID beerOrderId, boolean hasAllocationError, boolean hasPendingInventory) {
        BeerOrder beerOrder = beerOrderRepository.findOneById(beerOrderId);

        if (!hasAllocationError && !hasPendingInventory) {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_SUCCESS);
        } else if (hasAllocationError) {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_FAILED);
        } else {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_NO_INVENTORY);
        }
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEvent event) {
        StateMachine<BeerOrderStatus, BeerOrderEvent> sm = build(beerOrder);

        sm.sendEvent(
                MessageBuilder.withPayload(event)
                        .setHeader(BEER_ORDER_ID_HEADER, beerOrder.getId())
                        .build()
        );
    }

    private StateMachine<BeerOrderStatus, BeerOrderEvent> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatus, BeerOrderEvent> sm = stateMachineFactory.getStateMachine(beerOrder.getId());

        sm.stop();

        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(beerOrderStateInterceptor);
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
                });

        sm.start();

        return sm;
    }
}
