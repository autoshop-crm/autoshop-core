package com.vladko.autoshopcore.security;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.order.entity.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreSecurityServiceImplTest {

    private final CoreSecurityServiceImpl service = new CoreSecurityServiceImpl();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireCustomerAccessAllowsLinkedAuthUserId() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(77L, "client@test.com", Set.of("CUSTOMER"), "jti", Instant.parse("2026-05-20T10:00:00Z")),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        ));

        service.requireCustomerAccess(Order.builder().customer(Customer.builder().authUserId(77L).email("other@test.com").build()).build());
    }

    @Test
    void requireCustomerAccessFallsBackToEmailWhenAuthUserIdMissing() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(77L, "client@test.com", Set.of("CUSTOMER"), "jti", Instant.parse("2026-05-20T10:00:00Z")),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        ));

        service.requireCustomerAccess(Order.builder().customer(Customer.builder().email("client@test.com").build()).build());
    }

    @Test
    void requireCustomerAccessRejectsDifferentLinkedAuthUserId() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(77L, "client@test.com", Set.of("CUSTOMER"), "jti", Instant.parse("2026-05-20T10:00:00Z")),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        ));

        assertThatThrownBy(() -> service.requireCustomerAccess(Order.builder().customer(Customer.builder().authUserId(78L).email("client@test.com").build()).build()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Customer cannot access this order");
    }
}
