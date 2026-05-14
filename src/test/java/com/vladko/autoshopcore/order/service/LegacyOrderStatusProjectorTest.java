package com.vladko.autoshopcore.order.service;

import com.vladko.autoshopcore.order.entity.LegacyOrderStatus;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyOrderStatusProjectorTest {

    private final LegacyOrderStatusProjector projector = new LegacyOrderStatusProjector();

    @Test
    void shouldProjectBookingStatesToLegacyNew() {
        assertThat(projector.project(OrderStatus.WAITING_FOR_VISIT)).isEqualTo(LegacyOrderStatus.NEW);
        assertThat(projector.project(OrderStatus.ACCEPTED)).isEqualTo(LegacyOrderStatus.NEW);
    }

    @Test
    void shouldProjectOperationalStatesToLegacyInProgress() {
        assertThat(projector.project(OrderStatus.WAITING_FOR_OWNER_APPROVAL)).isEqualTo(LegacyOrderStatus.IN_PROGRESS);
        assertThat(projector.project(OrderStatus.READY_FOR_OWNER)).isEqualTo(LegacyOrderStatus.IN_PROGRESS);
    }

    @Test
    void shouldProjectTerminalStates() {
        assertThat(projector.project(OrderStatus.HANDED_OVER)).isEqualTo(LegacyOrderStatus.COMPLETED);
        assertThat(projector.project(OrderStatus.CANCELLED_NO_SHOW)).isEqualTo(LegacyOrderStatus.CANCELLED);
    }
}
