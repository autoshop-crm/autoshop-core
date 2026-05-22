package com.vladko.autoshopcore.security;

import com.vladko.autoshopcore.order.entity.Order;

public interface CoreSecurityService {
    CoreActor requireAnyStaff();
    CoreActor requireRoles(String... roles);
    void requireCustomerAccess(Order order);
    CoreActor currentActor();
}
