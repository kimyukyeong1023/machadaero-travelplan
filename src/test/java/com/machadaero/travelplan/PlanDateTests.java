package com.machadaero.travelplan;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import com.machadaero.travelplan.controller.PlanController;
import com.machadaero.travelplan.dto.PlanCreateRequestDto;
import com.machadaero.travelplan.dto.PlanUpdateRequestDto;
import com.machadaero.travelplan.entity.TravelPlan;
import com.machadaero.travelplan.entity.User;
import com.machadaero.travelplan.repository.PlanItemRepository;
import com.machadaero.travelplan.repository.TravelPlanRepository;
import com.machadaero.travelplan.repository.UserRepository;
import com.machadaero.travelplan.service.PlanItemService;
import com.machadaero.travelplan.service.PlanService;

class PlanDateTests {
    private final TravelPlanRepository plans = mock(TravelPlanRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PlanService service = new PlanService(plans, users, mock(PlanItemRepository.class));
    private final PlanController controller = new PlanController(service, mock(PlanItemService.class));

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("loginUserId", 7L);
        return request;
    }

    @Test
    void invalidCreateKeepsInputAndDoesNotSave() {
        PlanCreateRequestDto dto = new PlanCreateRequestDto();
        dto.setTitle("My trip");
        dto.setStartDate(LocalDate.of(2026, 9, 20));
        dto.setEndDate(LocalDate.of(2026, 9, 19));
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("planNew", controller.savePlan(dto, request(), model));
        assertSame(dto, model.get("formData"));
        assertNotNull(model.get("errorMessage"));
        assertNull(model.get("planDto"));
        verify(plans, never()).save(any());
    }

    @Test
    void invalidUpdateKeepsInputAndDoesNotMutateExistingPlan() {
        User user = new User();
        user.setId(7L);
        TravelPlan plan = new TravelPlan(user, "Original", null, null);
        plan.setId(1L);
        when(plans.findById(1L)).thenReturn(Optional.of(plan));
        PlanUpdateRequestDto dto = new PlanUpdateRequestDto();
        dto.setTitle("Changed");
        dto.setStartDate(LocalDate.of(2026, 9, 20));
        dto.setEndDate(LocalDate.of(2026, 9, 19));
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("planNew", controller.updatePlan(1L, dto, request(), model));
        assertSame(dto, model.get("formData"));
        assertNotNull(model.get("planDto"));
        assertNotNull(model.get("errorMessage"));
        assertEquals("Original", plan.getTitle());
        assertNull(plan.getStartDate());
        assertNull(plan.getEndDate());
        verify(plans, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({",", "2026-09-20,", ",2026-09-20",
                "2026-09-20,2026-09-20", "2026-09-20,2026-09-21"})
    void allowsMissingDatesSameDayAndForwardDates(String start, String end) {
        LocalDate startDate = start == null ? null : LocalDate.parse(start);
        LocalDate endDate = end == null ? null : LocalDate.parse(end);
        User user = new User();
        user.setId(7L);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        TravelPlan plan = new TravelPlan(user, "Original", null, null);
        when(plans.findById(1L)).thenReturn(Optional.of(plan));

        PlanCreateRequestDto create = new PlanCreateRequestDto();
        create.setStartDate(startDate);
        create.setEndDate(endDate);
        assertDoesNotThrow(() -> service.createPlan(7L, create));

        PlanUpdateRequestDto update = new PlanUpdateRequestDto();
        update.setStartDate(startDate);
        update.setEndDate(endDate);
        assertDoesNotThrow(() -> service.updatePlan(1L, 7L, update));
        assertEquals(startDate, plan.getStartDate());
        assertEquals(endDate, plan.getEndDate());
        verify(plans, times(2)).save(any());
    }
}
