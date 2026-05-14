package com.vladko.autoshopcore.security;

import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType;

public record CoreActor(Long actorId, OrderTimelineActorType actorType) {
}
