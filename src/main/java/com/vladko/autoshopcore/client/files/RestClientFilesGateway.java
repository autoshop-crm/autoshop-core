package com.vladko.autoshopcore.client.files;

import com.vladko.autoshopcore.integration.shared.ExternalApiAuthenticationException;
import com.vladko.autoshopcore.integration.shared.ExternalApiContractException;
import com.vladko.autoshopcore.integration.shared.ExternalApiUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RestClientFilesGateway implements FilesGateway {

    private final RestClient filesServiceRestClient;
    private final FilesServiceProperties properties;

    @Override
    public List<ExternalFileMetadataDTO> listByOwner(String ownerType, String ownerId) {
        try {
            ExternalFileListResponseDTO response = filesServiceRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(properties.listPath())
                            .queryParam("ownerType", ownerType)
                            .queryParam("ownerId", ownerId)
                            .queryParam("includeDeleted", false)
                            .build())
                    .retrieve()
                    .body(ExternalFileListResponseDTO.class);
            return response == null || response.getItems() == null ? List.of() : response.getItems();
        } catch (ResourceAccessException exception) {
            throw new ExternalApiUnavailableException("Files service is unavailable", exception);
        } catch (RestClientResponseException exception) {
            throw map(exception);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("Files service is unavailable", exception);
        }
    }

    @Override
    public ExternalFileMetadataDTO getById(String fileId) {
        try {
            return filesServiceRestClient.get()
                    .uri(properties.getPathTemplate(), fileId)
                    .retrieve()
                    .body(ExternalFileMetadataDTO.class);
        } catch (ResourceAccessException exception) {
            throw new ExternalApiUnavailableException("Files service is unavailable", exception);
        } catch (RestClientResponseException exception) {
            throw map(exception);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("Files service is unavailable", exception);
        }
    }

    @Override
    public ExternalPresignedDownloadUrlDTO createPresignedDownloadUrl(String fileId) {
        try {
            return filesServiceRestClient.post()
                    .uri(properties.presignedDownloadPathTemplate(), fileId)
                    .retrieve()
                    .body(ExternalPresignedDownloadUrlDTO.class);
        } catch (ResourceAccessException exception) {
            throw new ExternalApiUnavailableException("Files service is unavailable", exception);
        } catch (RestClientResponseException exception) {
            throw map(exception);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("Files service is unavailable", exception);
        }
    }

    private RuntimeException map(RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
            return new ExternalApiAuthenticationException("Files service rejected request");
        }
        if (HttpStatusCode.valueOf(exception.getStatusCode().value()).is4xxClientError()) {
            return new ExternalApiContractException(resolveMessage(exception, "Files service rejected request"), exception);
        }
        return new ExternalApiUnavailableException("Files service is unavailable", exception);
    }

    private String resolveMessage(RestClientResponseException exception, String defaultMessage) {
        String body = exception.getResponseBodyAsString();
        return body == null || body.isBlank() ? defaultMessage : body;
    }
}
