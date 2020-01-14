package guru.sfg.beer.order.service.services.clients.beer;

import guru.sfg.beer.order.service.services.clients.beer.model.BeerDto;

import java.util.Optional;
import java.util.UUID;

public interface BeerService {

    Optional<BeerDto> getBeerById(UUID id);

    Optional<BeerDto> getBeerByUpc(String upc);
}
