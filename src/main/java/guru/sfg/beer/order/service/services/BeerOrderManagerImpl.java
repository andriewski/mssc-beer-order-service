package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderStatus;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.ObjectNotFoundException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
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
        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);

        sendBeerOrderEvent(savedBeerOrder, BeerOrderEvent.VALIDATE_ORDER);

        return savedBeerOrder;
    }

    @Override
    public void processValidationResult(UUID beerOrderId, boolean isValid) {
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
            if (isValid) {
                sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_PASSED);

                BeerOrder validatedBeerOrder = beerOrderRepository.findById(beerOrderId)
                        .orElseThrow(() -> new ObjectNotFoundException(beerOrderId, "Beer"));

                sendBeerOrderEvent(validatedBeerOrder, BeerOrderEvent.ALLOCATE_ORDER);
            } else {
                sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_FAILED);
            }
        }, () -> log.error("processValidationResult# Object not found by id: {} ", beerOrderId));
    }

    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_SUCCESS);
            updateAllocatedQuantity(beerOrderDto);
        }, () -> log.error("beerOrderAllocationPassed# Object not found by id: {} ", beerOrderDto.getId()));
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_NO_INVENTORY);
            updateAllocatedQuantity(beerOrderDto);
        }, () -> log.error("beerOrderAllocationPendingInventory# Object not found by id: {}", beerOrderDto.getId()));
    }

    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_FAILED);
        }, () -> log.error("beerOrderAllocationFailed# Object not found by id: {}", beerOrderDto.getId()));
    }

    @Override
    public void pickupOrder(UUID beerOrderId) {
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.BEER_ORDER_PICKED_UP);
        }, () -> log.error("pickupOrder# Object not found by id: {}", beerOrderId));
    }

    @Override
    public void cancelOrder(UUID beerOrderId) {
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.CANCEL_ORDER);
        }, () -> log.error("cancelOrder# Object not found by id: {}", beerOrderId));
    }

    private void updateAllocatedQuantity(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(allocatedOrder -> {
            allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
                beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                    if (beerOrderLine.getBeerId().equals(beerOrderLineDto.getBeerId())) {
                        beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                    }
                });
            });

            beerOrderRepository.saveAndFlush(allocatedOrder);
        }, () -> log.error("updateAllocatedQuantity# Object not found by id: {}", beerOrderDto.getId()));
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
                    sma.resetStateMachine(
                            new DefaultStateMachineContext<>(
                                    beerOrder.getOrderStatus(),
                                    null,
                                    null,
                                    null
                            )
                    );
                });

        sm.start();

        return sm;
    }
}
