package guru.sfg.brewery.model.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidateBeerOrderResult {

    private UUID beerOrderId;
    private boolean isValid;
}
