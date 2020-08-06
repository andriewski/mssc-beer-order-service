package guru.sfg.beer.order.service.services;

import guru.sfg.brewery.model.CustomersPagedList;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomersPagedList getCustomers(Pageable pageable);
}
