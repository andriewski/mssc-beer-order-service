package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderStatus;
import guru.sfg.beer.order.service.sm.actions.AllocateOrderAction;
import guru.sfg.beer.order.service.sm.actions.ValidateOrderAction;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
@RequiredArgsConstructor
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<BeerOrderStatus, BeerOrderEvent> {

    private final ValidateOrderAction validateOrderAction;
    private final AllocateOrderAction allocateOrderAction;

    @Override
    public void configure(StateMachineStateConfigurer<BeerOrderStatus, BeerOrderEvent> states) throws Exception {
        states.withStates()
                .initial(BeerOrderStatus.NEW)
                .states(EnumSet.allOf(BeerOrderStatus.class))
                .end(BeerOrderStatus.DELIVERED)
                .end(BeerOrderStatus.DELIVERY_EXCEPTION)
                .end(BeerOrderStatus.VALIDATION_EXCEPTION)
                .end(BeerOrderStatus.ALLOCATION_EXCEPTION);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BeerOrderStatus, BeerOrderEvent> transitions) throws Exception {
        transitions.withExternal()
                .source(BeerOrderStatus.NEW).target(BeerOrderStatus.VALIDATION_PENDING)
                .event(BeerOrderEvent.VALIDATE_ORDER)
                .action(validateOrderAction)
            .and().withExternal()
                .source(BeerOrderStatus.NEW).target(BeerOrderStatus.VALIDATED)
                .event(BeerOrderEvent.VALIDATION_PASSED)
            .and().withExternal()
                .source(BeerOrderStatus.NEW).target(BeerOrderStatus.VALIDATION_EXCEPTION)
                .event(BeerOrderEvent.VALIDATION_FAILED)
            .and().withExternal()
                .source(BeerOrderStatus.VALIDATED).target(BeerOrderStatus.ALLOCATION_PENDING)
                .event(BeerOrderEvent.ALLOCATE_ORDER)
                .action(allocateOrderAction);
    }
}
