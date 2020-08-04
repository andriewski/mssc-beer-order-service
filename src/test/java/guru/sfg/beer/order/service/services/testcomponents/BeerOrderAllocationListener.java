package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocationOrderRequest;
import guru.sfg.brewery.model.events.AllocationOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;
    public final static String ALLOCATION_FAILED_KEY = "fail-allocation";
    public final static String PARTIAL_ALLOCATION_FAILED_KEY = "fail-partial-allocation";

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void handleMessage(Message<AllocationOrderRequest> msg) {
        AllocationOrderRequest request = msg.getPayload();
        boolean allocationFailed = ALLOCATION_FAILED_KEY.equals(request.getBeerOrderDto().getCustomerRef());
        boolean partialAllocation = PARTIAL_ALLOCATION_FAILED_KEY.equals(request.getBeerOrderDto().getCustomerRef());

        request.getBeerOrderDto().getBeerOrderLines().forEach(line -> {
            if (partialAllocation) {
                line.setQuantityAllocated(line.getOrderQuantity() - 1);
            } else {
                line.setQuantityAllocated(line.getOrderQuantity());
            }
        });

        jmsTemplate.convertAndSend(
                JmsConfig.ALLOCATE_ORDER_RESULT_QUEUE,
                AllocationOrderResult.builder()
                        .beerOrderDto(request.getBeerOrderDto())
                        .allocationError(allocationFailed)
                        .pendingInventory(partialAllocation)
                        .build()
        );
    }
}
