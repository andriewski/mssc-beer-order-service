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
    public final static String VALIDATION_FAILED_KEY = "fail-validation";
    public final static String VALIDATION_NO_RESPONSE = "validation-no-response";

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void handleMessage(Message<ValidateBeerOrderRequest> msg) {
        ValidateBeerOrderRequest request = msg.getPayload();
        boolean valid = !VALIDATION_FAILED_KEY.equals(request.getBeerOrderDto().getCustomerRef());
        boolean withResponse = !VALIDATION_NO_RESPONSE.equals(request.getBeerOrderDto().getCustomerRef());

        if (withResponse) {
            jmsTemplate.convertAndSend(
                    JmsConfig.VALIDATE_ORDER_RESULT_QUEUE,
                    ValidateBeerOrderResult.builder()
                            .isValid(valid)
                            .beerOrderId(request.getBeerOrderDto().getId())
                            .build()
            );
        }
    }
}
