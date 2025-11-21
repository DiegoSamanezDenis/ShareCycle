package com.sharecycle.ui;

import com.sharecycle.application.ResetSystemUseCase;
import com.sharecycle.domain.model.Operator;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.PricingPlan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemControllerTest {

    private ResetSystemUseCase resetSystemUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        resetSystemUseCase = Mockito.mock(ResetSystemUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SystemController(resetSystemUseCase)).build();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void operatorCanTriggerReset() throws Exception {
        Operator operator = new Operator("Op", "Addr", "op@test.com", "operator", "hash", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(operator, "token")
        );
        when(resetSystemUseCase.execute(operator.getUserId()))
                .thenReturn(new ResetSystemUseCase.ResetSummary(20, 10, 50));

        mockMvc.perform(post("/api/system/reset").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bikes", is(20)))
                .andExpect(jsonPath("$.stations", is(10)))
                .andExpect(jsonPath("$.docks", is(50)));

        verify(resetSystemUseCase).execute(operator.getUserId());
    }

    @Test
    void riderCannotTriggerReset() throws Exception {
        Rider rider = new Rider("Rider", "Addr", "rider@test.com", "rider", "hash", "pm", PricingPlan.PlanType.PAY_AS_YOU_GO);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(rider, "token")
        );

        mockMvc.perform(post("/api/system/reset"))
                .andExpect(status().isForbidden());

        verify(resetSystemUseCase, Mockito.never()).execute(any());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/system/reset"))
                .andExpect(status().isUnauthorized());
    }
}

