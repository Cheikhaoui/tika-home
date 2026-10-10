package com.tikahome.booking;

import com.tikahome.common.config.SecurityConfig;
import com.tikahome.pricing.QuoteIssue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web layer only: JSON binding, validation and error mapping. The service is mocked. */
@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    BookingService bookingService;

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"propertyId":"6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b","checkIn":"2030-01-10",
                         "checkOut":"2030-01-12","guests":0,
                         "guest":{"fullName":"","email":"not-an-email","language":"de"}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.guests").exists())
                .andExpect(jsonPath("$.errors['guest.email']").exists())
                .andExpect(jsonPath("$.errors['guest.fullName']").exists())
                .andExpect(jsonPath("$.errors['guest.language']").exists());
        verifyNoInteractions(bookingService);
    }

    @Test
    void dateConflictReturns409ProblemWithIssues() throws Exception {
        when(bookingService.request(any())).thenThrow(new BookingRejectedException(Set.of(QuoteIssue.DATES_UNAVAILABLE)));

        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"propertyId":"6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b","checkIn":"2030-01-10",
                         "checkOut":"2030-01-12","guests":2,
                         "guest":{"fullName":"Jane","email":"jane@example.com","language":"fr"}}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.issues[0]").value("DATES_UNAVAILABLE"));
    }
}
