package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatus;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.clients.beer.BeerServiceRestTemplateImpl;
import guru.sfg.beer.order.service.services.clients.beer.model.BeerDto;
import org.hibernate.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class BeerOrderManagerIt {

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    WireMockServer server;

    @Autowired
    ObjectMapper objectMapper;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();
    String upc = "12345";

    @TestConfiguration
    static class RestTemplateBuilderProvider {

        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer() {
            WireMockServer server = with(wireMockConfig().port(8083));
            server.start();

            return server;
        }
    }

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.save(
                Customer.builder()
                        .customerName("Test Customer")
                        .build()
        );
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testNewToAllocated() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc(upc)
                .build();

        server.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH_V1 + upc)
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))
        );
        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatus.ALLOCATED, foundOrder.getOrderStatus());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

        BeerOrder allocatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertNotNull(allocatedBeerOrder);
        assertEquals(BeerOrderStatus.ALLOCATED, allocatedBeerOrder.getOrderStatus());
        allocatedBeerOrder.getBeerOrderLines().forEach(line -> {
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });
    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();

        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(
                BeerOrderLine.builder()
                        .upc(upc)
                        .beerId(beerId)
                        .orderQuantity(1)
                        .quantityAllocated(1)
                        .beerOrder(beerOrder)
                        .build()

        );

        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }
}
