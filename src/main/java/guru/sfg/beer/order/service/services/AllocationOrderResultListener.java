package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocationOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationOrderResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RESULT_QUEUE)
    private void getAllocateResult(@Payload AllocationOrderResult allocationOrderResult) {
        boolean hasAllocationError  = allocationOrderResult.isAllocationError();
        boolean hasPendingInventory = allocationOrderResult.isPendingInventory();
        BeerOrderDto beerOrderDto = allocationOrderResult.getBeerOrderDto();

        beerOrderManager.processAllocationResult(beerOrderDto.getId(), hasAllocationError, hasPendingInventory);
    }
}
