package com.vladko.autoshopcore.security;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CoreSecurityServiceImpl implements CoreSecurityService {

    @Override
    public CoreActor requireAnyStaff() {
        return requireRoles("ADMIN", "MANAGER", "MECHANIC", "RECEPTIONIST");
    }

    @Override
    public CoreActor requireRoles(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new CoreActor(null, OrderTimelineActorType.SYSTEM);
        }
        Set<String> actualRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        for (String role : roles) {
            if (actualRoles.contains("ROLE_" + role)) {
                return actorFrom(authentication, role);
            }
        }
        throw new AccessDeniedException("Required role is missing");
    }

    @Override
    public void requireCustomerAccess(Order order) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        Set<String> actualRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (actualRoles.contains("ROLE_ADMIN") || actualRoles.contains("ROLE_MANAGER") || actualRoles.contains("ROLE_RECEPTIONIST") || actualRoles.contains("ROLE_MECHANIC")) {
            return;
        }
        if (!actualRoles.contains("ROLE_CUSTOMER")) {
            throw new AccessDeniedException("Customer role is required");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            throw new AccessDeniedException("Authenticated customer principal is required");
        }
        if (order.getCustomer() == null || order.getCustomer().getId() == null || !authenticatedUser.userId().equals(order.getCustomer().getId().longValue())) {
            throw new AccessDeniedException("Customer cannot access this order");
        }
    }

    @Override
    public CoreActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            return new CoreActor(null, OrderTimelineActorType.SYSTEM);
        }
        Set<String> actualRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (actualRoles.contains("ROLE_ADMIN")) return new CoreActor(authenticatedUser.userId(), OrderTimelineActorType.ADMIN);
        if (actualRoles.contains("ROLE_MANAGER")) return new CoreActor(authenticatedUser.userId(), OrderTimelineActorType.MANAGER);
        if (actualRoles.contains("ROLE_MECHANIC")) return new CoreActor(authenticatedUser.userId(), OrderTimelineActorType.MECHANIC);
        if (actualRoles.contains("ROLE_RECEPTIONIST")) return new CoreActor(authenticatedUser.userId(), OrderTimelineActorType.RECEPTIONIST);
        if (actualRoles.contains("ROLE_CUSTOMER")) return new CoreActor(authenticatedUser.userId(), OrderTimelineActorType.CUSTOMER);
        return new CoreActor(authenticatedUser.userId(), OrderTimelineActorType.SYSTEM);
    }

    private CoreActor actorFrom(Authentication authentication, String matchedRole) {
        Object principal = authentication.getPrincipal();
        Long userId = principal instanceof AuthenticatedUser authenticatedUser ? authenticatedUser.userId() : null;
        return new CoreActor(userId, switch (matchedRole) {
            case "ADMIN" -> OrderTimelineActorType.ADMIN;
            case "MANAGER" -> OrderTimelineActorType.MANAGER;
            case "MECHANIC" -> OrderTimelineActorType.MECHANIC;
            case "RECEPTIONIST" -> OrderTimelineActorType.RECEPTIONIST;
            case "CUSTOMER" -> OrderTimelineActorType.CUSTOMER;
            default -> OrderTimelineActorType.SYSTEM;
        });
    }
}
