package guru.sfg.brewery.model;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class CustomersPagedList extends PageImpl<CustomerDto> {

    public CustomersPagedList(List<CustomerDto> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public CustomersPagedList(List<CustomerDto> content) {
        super(content);
    }
}
