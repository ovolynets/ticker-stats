package de.ovolynets.tickerstats.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("default")
@AutoConfigureMockMvc
class APIControllerTest {

    private static final String SAMPLE_CONTENT = "{\"instrument\": \"IBM.N\", \"price\": 143.82, \"timestamp\": 1478192204000}";
    private static final String SAMPLE_CONTENT_MALFORMED = "{\"price\": 143.82, \"timestamp\": 1478192204000}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void checkPostOldTicker() throws Exception {
        mockMvc.perform(post("/api/ticks")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(SAMPLE_CONTENT))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void checkPostBadRequest() throws Exception {
        mockMvc.perform(post("/api/ticks")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(SAMPLE_CONTENT_MALFORMED))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkEmptyStatistics() throws Exception {
        mockMvc.perform(get("/api/statistics"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    void checkInstrumentNotFound() throws Exception {
        mockMvc.perform(get("/api/statistics/KO"))
                .andExpect(status().isNotFound());
    }

}
