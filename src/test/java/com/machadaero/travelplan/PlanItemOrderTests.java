package com.machadaero.travelplan;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import com.machadaero.travelplan.controller.PlanItemController;
import com.machadaero.travelplan.dto.PlanItemOrderRequestDto;
import com.machadaero.travelplan.entity.PlanItem;
import com.machadaero.travelplan.entity.TravelPlan;
import com.machadaero.travelplan.entity.User;
import com.machadaero.travelplan.repository.PlanItemRepository;
import com.machadaero.travelplan.repository.TravelPlanRepository;
import com.machadaero.travelplan.service.PlanItemService;

// Codex 작성: 순서 저장의 소유권·중복·누락·오래된 화면 검증과 기존 일정 내용 보존을 확인합니다.
class PlanItemOrderTests {
    private final PlanItemRepository items = mock(PlanItemRepository.class);
    private final TravelPlanRepository plans = mock(TravelPlanRepository.class);
    private final PlanItemService service = new PlanItemService(items, plans);
    private List<PlanItem> stored;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setId(7L);
        TravelPlan plan = new TravelPlan(owner, "Trip", null, null);
        plan.setId(1L);
        stored = Stream.of(10L, 20L, 30L).map(id -> {
            PlanItem item = new PlanItem();
            item.setId(id);
            item.setTravelPlan(plan);
            item.setSortOrder((int) (id / 5)); // 삭제로 생긴 순번 공백도 새 순서에서 정리되는지 확인합니다.
            item.setPlaceName("Place " + id);
            item.setVisitDate(LocalDate.of(2026, 9, 20));
            item.setMemo("Keep memo");
            return item;
        }).toList();
        when(plans.findByIdForOrderUpdate(1L)).thenReturn(Optional.of(plan));
        when(items.findByTravelPlanOrderBySortOrderAsc(plan)).thenReturn(stored);
    }

    private PlanItemOrderRequestDto request(List<Long> order) {
        PlanItemOrderRequestDto dto = new PlanItemOrderRequestDto();
        dto.setOriginalItemIds(List.of(10L, 20L, 30L));
        dto.setItemIds(order);
        return dto;
    }

    @Test
    void savesFinalOrderAndKeepsScheduleContents() {
        service.reorderPlanItems(7L, 1L, request(List.of(30L, 10L, 20L)));
        assertEquals(List.of(2, 3, 1), stored.stream().map(PlanItem::getSortOrder).toList());
        for (PlanItem item : stored) {
            assertEquals("Place " + item.getId(), item.getPlaceName());
            assertEquals(LocalDate.of(2026, 9, 20), item.getVisitDate());
            assertEquals("Keep memo", item.getMemo());
        }
        verify(items).saveAll(stored);
    }

    static Stream<List<Long>> invalidOrders() {
        return Stream.of(
                List.of(10L, 10L, 30L),
                List.of(10L, 20L),
                List.of(10L, 20L, 999L),
                List.of(10L, 20L, 30L, 40L),
                Arrays.asList(10L, null, 30L));
    }

    @ParameterizedTest
    @MethodSource("invalidOrders")
    void rejectsInvalidOrderWithoutChangingAnything(List<Long> order) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.reorderPlanItems(7L, 1L, request(order)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(List.of(2, 4, 6), stored.stream().map(PlanItem::getSortOrder).toList());
        verify(items, never()).saveAll(any());
    }

    @Test
    void rejectsStaleScreenWithoutChangingAnything() {
        PlanItemOrderRequestDto dto = request(List.of(30L, 10L, 20L));
        dto.setOriginalItemIds(List.of(20L, 10L, 30L));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.reorderPlanItems(7L, 1L, dto));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(List.of(2, 4, 6), stored.stream().map(PlanItem::getSortOrder).toList());
        verify(items, never()).saveAll(any());
    }

    @Test
    void rejectsMissingLists() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.reorderPlanItems(7L, 1L, new PlanItemOrderRequestDto()));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(items, never()).saveAll(any());
    }

    @Test
    void rejectsOtherUsersPlanBeforeReadingItems() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.reorderPlanItems(8L, 1L, request(List.of(30L, 10L, 20L))));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(items);
    }

    @Test
    void rejectsMissingPlan() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.reorderPlanItems(7L, 999L, request(List.of(30L, 10L, 20L))));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(items);
    }

    @Test
    void controllerRejectsExpiredSessionWithoutSaving() {
        PlanItemService mockedService = mock(PlanItemService.class);
        PlanItemController controller = new PlanItemController(mockedService);
        MockHttpServletRequest http = new MockHttpServletRequest();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.reorderPlanItems(1L, request(List.of(30L, 10L, 20L)), http));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertNull(http.getSession(false));
        verifyNoInteractions(mockedService);
    }

    @Test
    void controllerReturnsNoContentAfterSaving() {
        PlanItemService mockedService = mock(PlanItemService.class);
        PlanItemController controller = new PlanItemController(mockedService);
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.getSession().setAttribute("loginUserId", 7L);
        PlanItemOrderRequestDto dto = request(List.of(30L, 10L, 20L));
        assertEquals(HttpStatus.NO_CONTENT, controller.reorderPlanItems(1L, dto, http).getStatusCode());
        verify(mockedService).reorderPlanItems(7L, 1L, dto);
    }
}
