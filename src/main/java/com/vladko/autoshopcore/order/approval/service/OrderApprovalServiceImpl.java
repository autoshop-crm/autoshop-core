package com.vladko.autoshopcore.order.approval.service;

import com.vladko.autoshopcore.order.approval.dto.ApprovalRequestedPartDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalDecisionCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestResponseDTO;
import com.vladko.autoshopcore.order.approval.entity.*;
import com.vladko.autoshopcore.order.approval.repository.OrderApprovalDecisionRepository;
import com.vladko.autoshopcore.order.approval.repository.OrderApprovalRequestRepository;
import com.vladko.autoshopcore.order.approval.repository.OrderWorkProposalRepository;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.event.notification.OrderNotificationEventPublisher;
import com.vladko.autoshopcore.event.notification.OrderNotificationPayloadFactory;
import com.vladko.autoshopcore.order.exception.InvalidOrderStateException;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPart;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.exception.PartNotFoundException;
import com.vladko.autoshopcore.parts.repository.OrderRequestedPartRepository;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import com.vladko.autoshopcore.parts.service.OrderRequestedPartMapper;
import com.vladko.autoshopcore.security.CoreActor;
import com.vladko.autoshopcore.security.CoreSecurityService;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineEventType;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineVisibility;
import com.vladko.autoshopcore.order.timeline.service.OrderTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderApprovalServiceImpl implements OrderApprovalService {

    private final OrderRepository orderRepository;
    private final OrderWorkProposalRepository workProposalRepository;
    private final OrderApprovalRequestRepository approvalRequestRepository;
    private final OrderApprovalDecisionRepository approvalDecisionRepository;
    private final OrderRequestedPartRepository requestedPartRepository;
    private final PartRepository partRepository;
    private final OrderRequestedPartMapper requestedPartMapper;
    private final CoreSecurityService coreSecurityService;
    private final OrderTimelineService orderTimelineService;
    private final OrderNotificationPayloadFactory orderNotificationPayloadFactory;
    private final OrderNotificationEventPublisher orderNotificationEventPublisher;

    @Override
    @Transactional
    public OrderApprovalRequestResponseDTO requestApproval(Integer orderId, OrderApprovalRequestCreateDTO dto) {
        coreSecurityService.requireRoles("ADMIN", "MANAGER", "MECHANIC");
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        ensureApprovalEditable(order);
        if (approvalRequestRepository.existsByOrderIdAndStatus(orderId, OrderApprovalRequestStatus.OPEN)) {
            throw new OrderConflictException("Order already has an open approval request");
        }

        BigDecimal laborAmount = defaultIfNull(dto.getLaborAmount());
        BigDecimal partsAmount = defaultIfNull(dto.getPartsAmount());
        BigDecimal totalAmount = laborAmount.add(partsAmount);
        OrderApprovalType approvalType = dto.getRequestedPart() == null
                ? OrderApprovalType.EXTRA_WORK
                : laborAmount.compareTo(BigDecimal.ZERO) > 0 ? OrderApprovalType.MIXED_SCOPE_CHANGE : OrderApprovalType.PART_ONLY;

        OrderWorkProposal proposal = workProposalRepository.save(OrderWorkProposal.builder()
                .order(order)
                .status(OrderWorkProposalStatus.PENDING_APPROVAL)
                .approvalType(approvalType)
                .title(dto.getTitle().trim())
                .description(normalizeOptionalText(dto.getDescription(), 1000))
                .laborAmount(laborAmount)
                .partsAmount(partsAmount)
                .totalAmount(totalAmount)
                .requiresEveryExtraWorkApproval(dto.getRequiresApproval() != null
                        ? dto.getRequiresApproval()
                        : Boolean.TRUE.equals(order.getRequiresOwnerApprovalForEveryExtraWork()))
                .build());

        OrderApprovalRequest request = approvalRequestRepository.save(OrderApprovalRequest.builder()
                .order(order)
                .proposal(proposal)
                .approvalType(approvalType)
                .status(OrderApprovalRequestStatus.OPEN)
                .requestToken(UUID.randomUUID().toString())
                .requestedAmount(totalAmount)
                .requestedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(72 * 3600))
                .customerContactChannel(normalizeOptionalText(dto.getCustomerContactChannel(), 50))
                .build());

        OrderRequestedPart requestedPart = null;
        if (dto.getRequestedPart() != null) {
            requestedPart = requestedPartRepository.save(buildRequestedPart(order, request, dto.getRequestedPart()));
        }

        order.setStatus(OrderStatus.WAITING_FOR_OWNER_APPROVAL);
        orderRepository.save(order);
        CoreActor actor = safeActor();
        orderTimelineService.append(order, OrderTimelineEventType.APPROVAL_REQUESTED, OrderTimelineVisibility.BOTH, actor.actorType(), actor.actorId(), order.getStatus(), "Approval requested", null, "approval-request-" + request.getId());
        orderNotificationEventPublisher.publishOrderApprovalNeeded(orderNotificationPayloadFactory.orderApprovalNeeded(order, request.getId().longValue(), request.getApprovalType().name(), request.getRequestedAmount(), request.getExpiresAt()));
        return map(request, proposal, requestedPart);
    }

    @Override
    @Transactional
    public OrderApprovalRequestResponseDTO approve(Integer orderId, Integer requestId, OrderApprovalDecisionCreateDTO dto) {
        return decide(orderId, requestId, dto, OrderApprovalDecisionType.APPROVED);
    }

    @Override
    @Transactional
    public OrderApprovalRequestResponseDTO reject(Integer orderId, Integer requestId, OrderApprovalDecisionCreateDTO dto) {
        return decide(orderId, requestId, dto, OrderApprovalDecisionType.REJECTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderApprovalRequestResponseDTO> getByOrderId(Integer orderId) {
        orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        return approvalRequestRepository.findAllByOrderIdOrderByIdAsc(orderId).stream()
                .map(request -> map(request, request.getProposal(), findRequestedPart(request)))
                .toList();
    }

    private OrderApprovalRequestResponseDTO decide(Integer orderId, Integer requestId, OrderApprovalDecisionCreateDTO dto,
                                                   OrderApprovalDecisionType decisionType) {
        OrderApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new OrderConflictException("Approval request not found: " + requestId));
        if (!request.getOrder().getId().equals(orderId)) {
            throw new OrderConflictException("Approval request does not belong to order " + orderId);
        }
        Order order = request.getOrder();
        coreSecurityService.requireCustomerAccess(order);
        if (request.getStatus() != OrderApprovalRequestStatus.OPEN) {
            String key = request.getId() + "+" + dto.getDecisionToken();
            return approvalDecisionRepository.findByIdempotencyKey(key)
                    .map(existing -> map(request, request.getProposal(), findRequestedPart(request)))
                    .orElseThrow(() -> new OrderConflictException("Approval request is already terminal"));
        }

        String idempotencyKey = request.getId() + "+" + dto.getDecisionToken();
        if (approvalDecisionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return map(request, request.getProposal(), findRequestedPart(request));
        }

        approvalDecisionRepository.save(OrderApprovalDecision.builder()
                .approvalRequest(request)
                .decision(decisionType)
                .decisionToken(dto.getDecisionToken())
                .idempotencyKey(idempotencyKey)
                .comment(normalizeOptionalText(dto.getComment(), 1000))
                .decisionAt(Instant.now())
                .build());

        OrderWorkProposal proposal = request.getProposal();
        OrderRequestedPart requestedPart = findRequestedPart(request);
        CoreActor actor = safeActor();
        if (decisionType == OrderApprovalDecisionType.APPROVED) {
            request.setStatus(OrderApprovalRequestStatus.APPROVED);
            proposal.setStatus(OrderWorkProposalStatus.APPROVED);
            if (requestedPart != null) {
                requestedPart.setStatus(OrderRequestedPartStatus.OUT_OF_STOCK);
                requestedPartRepository.save(requestedPart);
                order.setStatus(OrderStatus.WAITING_FOR_PART);
                orderTimelineService.append(order, OrderTimelineEventType.WAITING_FOR_PART_ENTERED, OrderTimelineVisibility.BOTH, actor.actorType(), actor.actorId(), order.getStatus(), "Order is waiting for part", null, "waiting-for-part-" + request.getId());
                orderNotificationEventPublisher.publishOrderWaitingForPart(orderNotificationPayloadFactory.orderWaitingForPart(order, requestedPart.getId().longValue(), requestedPart.getName()));
            } else {
                order.setLaborTotal(order.getLaborTotal().add(proposal.getLaborAmount()));
                order.setStatus(order.getEmployee() == null ? OrderStatus.DIAGNOSIS_IN_PROGRESS : OrderStatus.REPAIR_IN_PROGRESS);
            }
        } else {
            request.setStatus(OrderApprovalRequestStatus.REJECTED);
            proposal.setStatus(OrderWorkProposalStatus.REJECTED);
            if (requestedPart != null) {
                requestedPart.setStatus(OrderRequestedPartStatus.CANCELLED);
                requestedPartRepository.save(requestedPart);
            }
            order.setStatus(order.getCheckedInAt() == null ? OrderStatus.WAITING_FOR_VISIT : OrderStatus.DIAGNOSIS_IN_PROGRESS);
        }
        request.setDecisionVersion(request.getDecisionVersion() + 1);
        approvalRequestRepository.save(request);
        workProposalRepository.save(proposal);
        orderRepository.save(order);
        orderTimelineService.append(order, decisionType == OrderApprovalDecisionType.APPROVED ? OrderTimelineEventType.APPROVAL_APPROVED : OrderTimelineEventType.APPROVAL_REJECTED, OrderTimelineVisibility.BOTH, actor.actorType(), actor.actorId(), order.getStatus(), decisionType == OrderApprovalDecisionType.APPROVED ? "Approval approved" : "Approval rejected", null, "approval-decision-" + request.getId() + "-" + decisionType);
        return map(request, proposal, requestedPart);
    }

    private OrderRequestedPart buildRequestedPart(Order order, OrderApprovalRequest request, ApprovalRequestedPartDTO dto) {
        Part matchedLocalPart = dto.getMatchedLocalPartId() == null ? null : partRepository.findById(dto.getMatchedLocalPartId())
                .orElseThrow(() -> new PartNotFoundException(dto.getMatchedLocalPartId()));
        return OrderRequestedPart.builder()
                .order(order)
                .approvalRequest(request)
                .articleNumber(normalizeRequiredText(dto.getArticleNumber(), true, 30))
                .brand(normalizeOptionalText(dto.getBrand(), 20))
                .name(normalizeRequiredText(dto.getName(), false, 100))
                .umapiArticleId(dto.getUmapiArticleId())
                .matchedLocalPart(matchedLocalPart)
                .requestedQuantity(dto.getQuantity())
                .status(OrderRequestedPartStatus.PENDING_CUSTOMER_APPROVAL)
                .build();
    }

    private OrderRequestedPart findRequestedPart(OrderApprovalRequest request) {
        return requestedPartRepository.findAllByOrderIdOrderByIdAsc(request.getOrder().getId()).stream()
                .filter(part -> part.getApprovalRequest() != null && part.getApprovalRequest().getId().equals(request.getId()))
                .findFirst()
                .orElse(null);
    }

    private void ensureApprovalEditable(Order order) {
        switch (order.getStatus()) {
            case ACCEPTED, DIAGNOSIS_IN_PROGRESS, REPAIR_IN_PROGRESS -> { }
            default -> throw new InvalidOrderStateException("Order in status '%s' cannot enter approval flow".formatted(order.getStatus()));
        }
    }

    private OrderApprovalRequestResponseDTO map(OrderApprovalRequest request,
                                                OrderWorkProposal proposal,
                                                OrderRequestedPart requestedPart) {
        OrderRequestedPartResponseDTO requestedPartResponse = requestedPart == null ? null : requestedPartMapper.map(requestedPart);
        return OrderApprovalRequestResponseDTO.builder()
                .requestId(request.getId())
                .orderId(request.getOrder().getId())
                .proposalId(proposal.getId())
                .approvalType(request.getApprovalType())
                .requestStatus(request.getStatus())
                .proposalStatus(proposal.getStatus())
                .requestToken(request.getRequestToken())
                .title(proposal.getTitle())
                .description(proposal.getDescription())
                .laborAmount(proposal.getLaborAmount())
                .partsAmount(proposal.getPartsAmount())
                .totalAmount(proposal.getTotalAmount())
                .requestedAt(request.getRequestedAt())
                .expiresAt(request.getExpiresAt())
                .customerContactChannel(request.getCustomerContactChannel())
                .requestedPart(requestedPartResponse)
                .build();
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private CoreActor safeActor() {
        CoreActor actor = coreSecurityService.currentActor();
        return actor == null ? new CoreActor(null, com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType.SYSTEM) : actor;
    }

    private String normalizeRequiredText(String value, boolean uppercase, int maxLen) {
        String normalized = normalizeOptionalText(value, maxLen);
        if (normalized == null) {
            throw new IllegalArgumentException("Value must not be blank");
        }
        return uppercase ? normalized.toUpperCase(Locale.ROOT) : normalized;
    }

    private String normalizeOptionalText(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLen) {
            normalized = normalized.substring(0, maxLen);
        }
        return normalized;
    }
}
