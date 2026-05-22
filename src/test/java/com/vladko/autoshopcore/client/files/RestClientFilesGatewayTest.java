package com.vladko.autoshopcore.client.files;

import com.vladko.autoshopcore.integration.shared.ExternalApiUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientFilesGatewayTest {

    @Test
    void listByOwnerShouldReadPagedItemsResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://files.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientFilesGateway gateway = new RestClientFilesGateway(
                builder.build(),
                new FilesServiceProperties("http://files.test", "/api/files", "/api/files/{fileId}", "/api/files/{fileId}/presigned-download-url")
        );

        server.expect(requestTo("http://files.test/api/files?ownerType=CUSTOMER&ownerId=16&includeDeleted=false"))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            {
                              "fileId": "f-1",
                              "filename": "report.pdf",
                              "ownerType": "CUSTOMER",
                              "ownerId": "16"
                            }
                          ],
                          "page": 0,
                          "size": 20,
                          "totalElements": 1
                        }
                        """, MediaType.APPLICATION_JSON));

        List<ExternalFileMetadataDTO> files = gateway.listByOwner("CUSTOMER", "16");

        assertThat(files).hasSize(1);
        assertThat(files.get(0).getFileId()).isEqualTo("f-1");
        server.verify();
    }

    @Test
    void listByOwnerShouldThrowUnavailableWhenPayloadShapeIsUnexpected() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://files.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientFilesGateway gateway = new RestClientFilesGateway(
                builder.build(),
                new FilesServiceProperties("http://files.test", "/api/files", "/api/files/{fileId}", "/api/files/{fileId}/presigned-download-url")
        );

        server.expect(requestTo("http://files.test/api/files?ownerType=CUSTOMER&ownerId=16&includeDeleted=false"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.listByOwner("CUSTOMER", "16"))
                .isInstanceOf(ExternalApiUnavailableException.class);
    }

    @Test
    void listByOwnerShouldMapFilesServiceFieldNames() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://files.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientFilesGateway gateway = new RestClientFilesGateway(
                builder.build(),
                new FilesServiceProperties("http://files.test", "/api/files", "/api/files/{fileId}", "/api/files/{fileId}/presigned-download-url")
        );

        server.expect(requestTo("http://files.test/api/files?ownerType=ORDER&ownerId=6&includeDeleted=false"))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            {
                              "id": "ef90a674-5e3b-4178-a6a4-926a889c435c",
                              "category": "ORDER_DOCUMENT",
                              "ownerType": "ORDER",
                              "ownerId": "6",
                              "originalFilename": "jde3sqn0lha51.png",
                              "contentType": "image/png"
                            }
                          ],
                          "page": 0,
                          "size": 20,
                          "totalElements": 1
                        }
                        """, MediaType.APPLICATION_JSON));

        List<ExternalFileMetadataDTO> files = gateway.listByOwner("ORDER", "6");

        assertThat(files).hasSize(1);
        assertThat(files.get(0).getFileId()).isEqualTo("ef90a674-5e3b-4178-a6a4-926a889c435c");
        assertThat(files.get(0).getFilename()).isEqualTo("jde3sqn0lha51.png");
        server.verify();
    }
}
