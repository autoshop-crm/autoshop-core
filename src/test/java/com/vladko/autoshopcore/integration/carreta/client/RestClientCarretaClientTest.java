package com.vladko.autoshopcore.integration.carreta.client;

import com.vladko.autoshopcore.integration.carreta.config.CarretaProperties;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderCreateRequest;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderResponse;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaSearchResponse;
import com.vladko.autoshopcore.integration.shared.ExternalApiAuthenticationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class RestClientCarretaClientTest {

    @Test
    void searchShouldCallCarretaApiAndMapResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.carreta.ru");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientCarretaClient client = new RestClientCarretaClient(builder.build(), properties());

        server.expect(requestTo(containsString("/v1/search/")))
                .andExpect(requestTo(containsString("api_key=test-key")))
                .andExpect(requestTo(containsString("q=OC47")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "objects": [
                            {
                              "_": "signature",
                              "code": "OC47",
                              "desc": "",
                              "is_cross": false,
                              "maker": "KNECHT/MAHLE",
                              "min_qty": 1,
                              "name": "Oil filter",
                              "period_max": 7,
                              "period_min": 7,
                              "price": "152.02",
                              "qty": "500",
                              "stat": 97,
                              "source": "57"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        CarretaSearchResponse response = client.search("OC47");

        assertThat(response.getObjects()).hasSize(1);
        assertThat(response.getObjects().get(0).getCode()).isEqualTo("OC47");
        assertThat(response.getObjects().get(0).getPositionSignature()).isEqualTo("signature");
        assertThat(response.getObjects().get(0).getMinQty()).isEqualTo(1);
        server.verify();
    }

    @Test
    void createOrderShouldUseTestModeWhenRequested() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.carreta.ru");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientCarretaClient client = new RestClientCarretaClient(builder.build(), properties());

        server.expect(requestTo(containsString("/v1/order/")))
                .andExpect(requestTo(containsString("api_key=test-key")))
                .andExpect(requestTo(containsString("test=on")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withCreatedEntity(null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "id": 189330,
                                  "number": "A111342-1",
                                  "status": 2,
                                  "status_display": "Получен в заказ",
                                  "code": "OC47",
                                  "maker": "KNECHT/MAHLE",
                                  "name": "Oil filter",
                                  "price": "152.02",
                                  "order_qty": 1,
                                  "total": "152.02"
                                }
                                """));

        CarretaOrderResponse response = client.createOrder(CarretaOrderCreateRequest.builder()
                .positionSignature("signature")
                .code("OC47")
                .maker("KNECHT/MAHLE")
                .name("Oil filter")
                .price("152.02")
                .periodMin(7)
                .periodMax(7)
                .minQty(1)
                .qty("500")
                .orderQuantity(1)
                .clientComment("AS-1")
                .build(), true);

        assertThat(response.getId()).isEqualTo(189330);
        assertThat(response.getStatusDisplay()).isEqualTo("Получен в заказ");
        server.verify();
    }

    @Test
    void searchShouldMapUnauthorizedToAuthenticationException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.carreta.ru");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientCarretaClient client = new RestClientCarretaClient(builder.build(), properties());

        server.expect(requestTo(containsString("/v1/search/")))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.search("OC47"))
                .isInstanceOf(ExternalApiAuthenticationException.class)
                .hasMessage("Carreta authentication failed");

        server.verify();
    }

    private CarretaProperties properties() {
        return new CarretaProperties(
                "https://api.carreta.ru",
                "test-key",
                "",
                true,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                new CarretaProperties.Cache(Duration.ofMinutes(30)),
                new CarretaProperties.Retry(3, Duration.ofMillis(10))
        );
    }
}
