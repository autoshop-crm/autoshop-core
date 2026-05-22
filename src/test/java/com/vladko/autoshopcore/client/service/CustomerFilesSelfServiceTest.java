package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.files.ExternalFileMetadataDTO;
import com.vladko.autoshopcore.client.files.ExternalPresignedDownloadUrlDTO;
import com.vladko.autoshopcore.client.files.FilesGateway;
import com.vladko.autoshopcore.customerauth.service.CustomerIdentityLinkService;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.service.OrderService;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerFilesSelfServiceTest {

    @Mock private CustomerIdentityLinkService customerIdentityLinkService;
    @Mock private VehicleService vehicleService;
    @Mock private OrderService orderService;
    @Mock private FilesGateway filesGateway;

    @InjectMocks private CustomerFilesSelfServiceImpl customerFilesSelfService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentCustomerDocumentsShouldListCustomerOwnedFiles() {
        authenticateCustomer();
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").build());
        ExternalFileMetadataDTO file = new ExternalFileMetadataDTO();
        file.setFileId("f1");
        file.setOwnerType("CUSTOMER");
        file.setOwnerId("9");
        file.setFilename("passport.pdf");
        when(filesGateway.listByOwner("CUSTOMER", "9")).thenReturn(List.of(file));

        var response = customerFilesSelfService.getCurrentCustomerDocuments();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getFileId()).isEqualTo("f1");
    }

    @Test
    void orderDocumentsShouldRequireOrderAccessThroughOrderService() {
        authenticateCustomer();
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").build());
        when(orderService.getById(100)).thenReturn(OrderResponseDTO.builder().id(100).build());
        when(filesGateway.listByOwner("ORDER", "100")).thenReturn(List.of());

        customerFilesSelfService.getCurrentCustomerOrderDocuments(100);

        verify(orderService).getById(100);
    }

    @Test
    void presignedDownloadShouldValidateVehicleOwnership() {
        authenticateCustomer();
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").build());
        ExternalFileMetadataDTO file = new ExternalFileMetadataDTO();
        file.setFileId("f2");
        file.setOwnerType("VEHICLE");
        file.setOwnerId("15");
        when(filesGateway.getById("f2")).thenReturn(file);
        when(vehicleService.getById(15)).thenReturn(VehicleResponseDTO.builder().id(15).customerId(9).build());
        ExternalPresignedDownloadUrlDTO url = new ExternalPresignedDownloadUrlDTO();
        url.setUrl("http://files/presigned");
        when(filesGateway.createPresignedDownloadUrl("f2")).thenReturn(url);

        var response = customerFilesSelfService.getPresignedDownloadUrl("f2");

        assertThat(response.getUrl()).isEqualTo("http://files/presigned");
    }

    private void authenticateCustomer() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(44L, "client@test.com", Set.of("CUSTOMER"), "jti", java.time.Instant.parse("2026-05-20T10:00:00Z")),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        ));
    }
}
