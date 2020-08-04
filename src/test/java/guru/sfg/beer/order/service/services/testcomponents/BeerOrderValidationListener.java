package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import guru.sfg.brewery.model.events.ValidateBeerOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void handleMessage(Message<ValidateBeerOrderRequest> msg) {
        ValidateBeerOrderRequest request = msg.getPayload();

        jmsTemplate.convertAndSend(
                JmsConfig.VALIDATE_ORDER_RESULT_QUEUE,
                ValidateBeerOrderResult.builder()
                        .isValid(true)
                        .beerOrderId(request.getBeerOrderDto().getId())
                        .build()
        );
    }
}
