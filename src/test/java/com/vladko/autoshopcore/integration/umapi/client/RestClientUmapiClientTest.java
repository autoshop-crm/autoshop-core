package com.vladko.autoshopcore.integration.umapi.client;

import com.vladko.autoshopcore.integration.umapi.config.UmapiProperties;
import com.vladko.autoshopcore.integration.umapi.dto.UmapiAnalogsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientUmapiClientTest {

    @Test
    void findAnalogsShouldSendAppKeyHeaderAndMapArticleFields() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.umapi.ru");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientUmapiClient client = new RestClientUmapiClient(builder.build(), properties());

        server.expect(requestTo(containsString("/v2/autocatalog/ru-WWW/Analogs/OC90/KNECHT")))
                .andExpect(requestTo(containsString("limit=10")))
                .andExpect(header("X-App-Key", "umapi-key"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "rows": 1,
                          "limit": 10,
                          "offset": 0,
                          "data": [
                            {
                              "ART_ID": 123,
                              "ARTICLE_NR": "OC90",
                              "SUP_ID": 42,
                              "BRAND": "KNECHT",
                              "COMPLETE_DES": "Oil filter",
                              "DES": "Filter",
                              "STATUS_DES": "Normal"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        UmapiAnalogsResponse response = client.findAnalogs("OC90", "KNECHT", 10, 0);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getArticleId()).isEqualTo(123);
        assertThat(response.getData().get(0).getBrand()).isEqualTo("KNECHT");
        server.verify();
    }

    @Test
    void getManufacturersShouldCallCatalogManufacturersEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.umapi.ru");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientUmapiClient client = new RestClientUmapiClient(builder.build(), properties());

        server.expect(requestTo(containsString("/v2/autocatalog/ru-WWW/Manufacturers")))
                .andExpect(requestTo(containsString("type=PC")))
                .andExpect(requestTo(containsString("popular=true")))
                .andExpect(header("X-App-Key", "umapi-key"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          { "MFA_ID": 111, "MANUFACTURER": "TOYOTA" }
                        ]
                        """, MediaType.APPLICATION_JSON));

        var response = client.getManufacturers("PC", true);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getMfaId()).isEqualTo(111);
        server.verify();
    }

    @Test
    void getModelSeriesShouldPassManufacturerId() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.umapi.ru");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientUmapiClient client = new RestClientUmapiClient(builder.build(), properties());

        server.expect(requestTo(containsString("/v2/autocatalog/ru-WWW/ModelSeries")))
                .andExpect(requestTo(containsString("MFA_ID=111")))
                .andExpect(header("X-App-Key", "umapi-key"))
                .andRespond(withSuccess("""
                        [
                          { "MFA_ID": 111, "MS_ID": 222, "MODEL_SERIES": "CAMRY" }
                        ]
                        """, MediaType.APPLICATION_JSON));

        var response = client.getModelSeries("PC", 111);

        assertThat(response.get(0).getModelSeriesId()).isEqualTo(222);
        server.verify();
    }

    @Test
    void getPassengerModificationsShouldPassModelSeriesId() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.umapi.ru");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientUmapiClient client = new RestClientUmapiClient(builder.build(), properties());

        server.expect(requestTo(containsString("/v2/autocatalog/ru-WWW/Passangers")))
                .andExpect(requestTo(containsString("MS_ID=222")))
                .andExpect(header("X-App-Key", "umapi-key"))
                .andRespond(withSuccess("""
                        [
                          { "PC_ID": 333, "PASSENGER_CAR": "Camry 2.5", "POWER_PS": 181 }
                        ]
                        """, MediaType.APPLICATION_JSON));

        var response = client.getPassengerModifications("PC", 222);

        assertThat(response.get(0).getModificationId()).isEqualTo(333);
        server.verify();
    }

    @Test
    void getFuseProductGroupsShouldPassModificationId() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.umapi.ru");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientUmapiClient client = new RestClientUmapiClient(builder.build(), properties());

        server.expect(requestTo(containsString("/v2/autocatalog/ru-WWW/Fuse")))
                .andExpect(requestTo(containsString("ID=333")))
                .andExpect(header("X-App-Key", "umapi-key"))
                .andRespond(withSuccess("""
                        [
                          { "PT_ID": 7, "DES": "Масляный фильтр", "NORM_DES": "масляный фильтр" }
                        ]
                        """, MediaType.APPLICATION_JSON));

        var response = client.getFuseProductGroups("PC", 333);

        assertThat(response.get(0).getProductGroupId()).isEqualTo(7);
        server.verify();
    }

    @Test
    void getArticlesShouldPassProductGroupsModificationAndPaging() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.umapi.ru");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientUmapiClient client = new RestClientUmapiClient(builder.build(), properties());

        server.expect(requestTo(containsString("/v2/autocatalog/ru-WWW/Articles")))
                .andExpect(requestTo(containsString("ID=333")))
                .andExpect(requestTo(containsString("limit=10")))
                .andExpect(requestTo(containsString("offset=0")))
                .andExpect(requestTo(containsString("SUP_ID=42")))
                .andExpect(header("X-App-Key", "umapi-key"))
                .andRespond(withSuccess("""
                        {
                          "rows": 1,
                          "limit": 10,
                          "offset": 0,
                          "data": [
                            { "ART_ID": 987, "ARTICLE_NR": "90915YZZE1", "BRAND": "TOYOTA" }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.getArticles("PC", List.of(7, 8), 333, 42, 10, 0);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getArticleId()).isEqualTo(987);
        server.verify();
    }

    private UmapiProperties properties() {
        return new UmapiProperties(
                "https://api.umapi.ru",
                "umapi-key",
                "ru",
                "WWW",
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                new UmapiProperties.Cache(Duration.ofHours(6)),
                new UmapiProperties.Retry(3, Duration.ofMillis(10))
        );
    }
}
