package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.web.mappers.CustomerMapper;
import guru.sfg.brewery.model.CustomersPagedList;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final CustomerRepository customerRepository;

    @Override
    public CustomersPagedList getCustomers(Pageable pageable) {
        Page<Customer> customersPage = customerRepository.findAll(pageable);

        return new CustomersPagedList(
                customersPage.stream()
                        .map(customerMapper::toCustomerDto)
                        .collect(Collectors.toList()),
                PageRequest.of(
                        customersPage.getPageable().getPageNumber(),
                        customersPage.getPageable().getPageSize()
                ),
                customersPage.getTotalElements()
        );
    }
}
