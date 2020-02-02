package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateBeerOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeerOrderValidationResultListener {

    private final BeerOrderManager beerOrderManager;

    @Transactional
    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESULT_QUEUE)
    public void getValidationResult(@Payload ValidateBeerOrderResult validateBeerOrderResult) {
        log.debug("Validation Result for Order Id: " + validateBeerOrderResult.getBeerOrderId());

        beerOrderManager.applyValidationResult(validateBeerOrderResult.getBeerOrderId(), validateBeerOrderResult.isValid());
    }
}
