package com.machadaero.travelplan;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import com.machadaero.travelplan.controller.PlanController;
import com.machadaero.travelplan.dto.PlanResponseDto;
import com.machadaero.travelplan.entity.TravelPlan;
import com.machadaero.travelplan.repository.PlanItemRepository;
import com.machadaero.travelplan.repository.TravelPlanRepository;
import com.machadaero.travelplan.repository.UserRepository;
import com.machadaero.travelplan.service.PlanItemService;
import com.machadaero.travelplan.service.PlanService;

class PlanPaginationTests {
    // Codex 수정: 서버는 로그인 사용자의 계획 전체를 DTO로 전달하고, 일정은 조회하지 않습니다.
    @ParameterizedTest
    @ValueSource(ints = {0, 2, 10, 11, 21})
    void returnsAllPlansForBrowserPagination(int total) {
        List<TravelPlan> plans = IntStream.rangeClosed(1, total).mapToObj(index -> {
            TravelPlan plan = new TravelPlan();
            plan.setId((long) index);
            plan.setTitle("Plan " + index);
            return plan;
        }).toList();
        TravelPlanRepository repository = mock(TravelPlanRepository.class);
        PlanItemRepository items = mock(PlanItemRepository.class);
        PlanItemService itemService = mock(PlanItemService.class);
        when(repository.findAllByUser_Id(7L)).thenReturn(plans);
        PlanService service = new PlanService(repository, mock(UserRepository.class), items);
        PlanController controller = new PlanController(service, itemService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("loginUserId", 7L);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("plans.html", controller.plans(request, model));
        List<?> displayed = (List<?>) model.get("planList");
        assertEquals(total, displayed.size());
        assertTrue(displayed.stream().allMatch(PlanResponseDto.class::isInstance));
        verify(repository).findAllByUser_Id(7L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(items, itemService);
    }

    // Codex 작성: 전체 조회로 바뀌어도 로그인하지 않은 사용자의 조회는 차단합니다.
    @Test
    void redirectsAnonymousUserWithoutQueryingPlans() {
        PlanService service = mock(PlanService.class);
        PlanController controller = new PlanController(service, mock(PlanItemService.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertEquals("redirect:/login", controller.plans(request, new ExtendedModelMap()));
        request.getSession();
        assertEquals("redirect:/login", controller.plans(request, new ExtendedModelMap()));
        verifyNoInteractions(service);
    }
}
