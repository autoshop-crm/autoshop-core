package com.vladko.autoshopcore.order.approval.service;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.order.approval.dto.ApprovalRequestedPartDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalDecisionCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestResponseDTO;
import com.vladko.autoshopcore.order.approval.entity.OrderApprovalRequest;
import com.vladko.autoshopcore.order.approval.entity.OrderApprovalRequestStatus;
import com.vladko.autoshopcore.order.approval.entity.OrderWorkProposal;
import com.vladko.autoshopcore.order.approval.entity.OrderWorkProposalStatus;
import com.vladko.autoshopcore.order.approval.repository.OrderApprovalDecisionRepository;
import com.vladko.autoshopcore.order.approval.repository.OrderApprovalRequestRepository;
import com.vladko.autoshopcore.order.approval.repository.OrderWorkProposalRepository;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPart;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import com.vladko.autoshopcore.parts.repository.OrderRequestedPartRepository;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import com.vladko.autoshopcore.parts.service.OrderRequestedPartMapper;
import com.vladko.autoshopcore.security.CoreSecurityService;
import com.vladko.autoshopcore.order.timeline.service.OrderTimelineService;
import com.vladko.autoshopcore.event.notification.OrderNotificationPayloadFactory;
import com.vladko.autoshopcore.event.notification.OrderNotificationEventPublisher;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderApprovalServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderWorkProposalRepository workProposalRepository;
    @Mock private OrderApprovalRequestRepository approvalRequestRepository;
    @Mock private OrderApprovalDecisionRepository approvalDecisionRepository;
    @Mock private OrderRequestedPartRepository requestedPartRepository;
    @Mock private PartRepository partRepository;
    @Mock private CoreSecurityService coreSecurityService;
    @Mock private OrderTimelineService orderTimelineService;
    @Mock private OrderNotificationPayloadFactory orderNotificationPayloadFactory;
    @Mock private OrderNotificationEventPublisher orderNotificationEventPublisher;

    private OrderApprovalService service;

    @BeforeEach
    void setUp() {
        lenient().when(coreSecurityService.currentActor()).thenReturn(new com.vladko.autoshopcore.security.CoreActor(1L, com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType.MANAGER));
        lenient().doNothing().when(coreSecurityService).requireCustomerAccess(any());
        lenient().when(coreSecurityService.requireRoles(any())).thenReturn(new com.vladko.autoshopcore.security.CoreActor(1L, com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType.MANAGER));
        service = new OrderApprovalServiceImpl(
                orderRepository,
                workProposalRepository,
                approvalRequestRepository,
                approvalDecisionRepository,
                requestedPartRepository,
                partRepository,
                new OrderRequestedPartMapper(),
                coreSecurityService,
                orderTimelineService,
                orderNotificationPayloadFactory,
                orderNotificationEventPublisher
        );
    }

    @Test
    void requestApprovalShouldMoveOrderToWaitingForOwnerApproval() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order order = Order.builder().id(5).customer(customer).vehicle(vehicle).status(OrderStatus.DIAGNOSIS_IN_PROGRESS).build();
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(approvalRequestRepository.existsByOrderIdAndStatus(5, OrderApprovalRequestStatus.OPEN)).thenReturn(false);
        when(workProposalRepository.save(any(OrderWorkProposal.class))).thenAnswer(inv -> {
            OrderWorkProposal proposal = inv.getArgument(0);
            proposal.setId(11);
            return proposal;
        });
        when(approvalRequestRepository.save(any(OrderApprovalRequest.class))).thenAnswer(inv -> {
            OrderApprovalRequest request = inv.getArgument(0);
            request.setId(12);
            return request;
        });

        OrderApprovalRequestResponseDTO response = service.requestApproval(5, requestDto(false));

        assertThat(response.getRequestStatus()).isEqualTo(OrderApprovalRequestStatus.OPEN);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_OWNER_APPROVAL);
        verify(orderRepository).save(order);
    }

    @Test
    void approveShouldMoveOrderToWaitingForPartForPartProposal() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order order = Order.builder().id(5).customer(customer).vehicle(vehicle).status(OrderStatus.WAITING_FOR_OWNER_APPROVAL).build();
        OrderWorkProposal proposal = OrderWorkProposal.builder()
                .id(11).order(order).status(OrderWorkProposalStatus.PENDING_APPROVAL)
                .title("Need part").laborAmount(BigDecimal.ZERO).partsAmount(new BigDecimal("50.00")).totalAmount(new BigDecimal("50.00"))
                .build();
        OrderApprovalRequest request = OrderApprovalRequest.builder()
                .id(12).order(order).proposal(proposal).status(OrderApprovalRequestStatus.OPEN).requestToken("tok")
                .build();
        OrderRequestedPart part = OrderRequestedPart.builder().id(13).order(order).approvalRequest(request).status(OrderRequestedPartStatus.PENDING_CUSTOMER_APPROVAL).build();
        when(approvalRequestRepository.findById(12)).thenReturn(Optional.of(request));
        when(approvalDecisionRepository.findByIdempotencyKey("12+dec-1")).thenReturn(Optional.empty());
        when(requestedPartRepository.findAllByOrderIdOrderByIdAsc(5)).thenReturn(List.of(part));

        OrderApprovalRequestResponseDTO response = service.approve(5, 12, decision("dec-1"));

        assertThat(response.getRequestStatus()).isEqualTo(OrderApprovalRequestStatus.APPROVED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_PART);
        assertThat(part.getStatus()).isEqualTo(OrderRequestedPartStatus.OUT_OF_STOCK);
    }

    @Test
    void rejectShouldBeIdempotentForSameDecisionToken() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order order = Order.builder().id(5).customer(customer).vehicle(vehicle).status(OrderStatus.WAITING_FOR_OWNER_APPROVAL).checkedInAt(java.time.Instant.now()).build();
        OrderWorkProposal proposal = OrderWorkProposal.builder()
                .id(11).order(order).status(OrderWorkProposalStatus.PENDING_APPROVAL)
                .title("Extra work").laborAmount(new BigDecimal("30.00")).partsAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("30.00"))
                .build();
        OrderApprovalRequest request = OrderApprovalRequest.builder()
                .id(12).order(order).proposal(proposal).status(OrderApprovalRequestStatus.OPEN).requestToken("tok")
                .build();
        when(approvalRequestRepository.findById(12)).thenReturn(Optional.of(request));
        when(approvalDecisionRepository.findByIdempotencyKey("12+same")).thenReturn(Optional.empty(), Optional.of(mock(com.vladko.autoshopcore.order.approval.entity.OrderApprovalDecision.class)));
        when(requestedPartRepository.findAllByOrderIdOrderByIdAsc(5)).thenReturn(List.of());

        service.reject(5, 12, decision("same"));
        OrderApprovalRequestResponseDTO repeated = service.reject(5, 12, decision("same"));

        assertThat(repeated.getRequestStatus()).isEqualTo(OrderApprovalRequestStatus.REJECTED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DIAGNOSIS_IN_PROGRESS);
    }

    @Test
    void requestApprovalShouldRejectSecondOpenRequest() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order order = Order.builder().id(5).customer(customer).vehicle(vehicle).status(OrderStatus.DIAGNOSIS_IN_PROGRESS).build();
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(approvalRequestRepository.existsByOrderIdAndStatus(5, OrderApprovalRequestStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> service.requestApproval(5, requestDto(false)))
                .isInstanceOf(OrderConflictException.class)
                .hasMessage("Order already has an open approval request");
    }

    private OrderApprovalRequestCreateDTO requestDto(boolean withPart) {
        OrderApprovalRequestCreateDTO dto = new OrderApprovalRequestCreateDTO();
        dto.setTitle("Extra work");
        dto.setDescription("Need additional scope");
        dto.setLaborAmount(new BigDecimal("25.00"));
        dto.setPartsAmount(withPart ? new BigDecimal("50.00") : BigDecimal.ZERO);
        dto.setCustomerContactChannel("PHONE");
        if (withPart) {
            ApprovalRequestedPartDTO part = new ApprovalRequestedPartDTO();
            part.setArticleNumber("OF123");
            part.setBrand("BOSCH");
            part.setName("Oil Filter");
            part.setQuantity(1);
            dto.setRequestedPart(part);
        }
        return dto;
    }

    private OrderApprovalDecisionCreateDTO decision(String token) {
        OrderApprovalDecisionCreateDTO dto = new OrderApprovalDecisionCreateDTO();
        dto.setDecisionToken(token);
        dto.setComment("ok");
        return dto;
    }
}
