package com.jandi.band_backend.integration;

import com.jandi.band_backend.config.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ApiContractIntegrationTest {
    private static final Set<String> DOCUMENTATION = Set.of(
            "description", "summary", "example", "examples", "title", "tags", "externalDocs");

    @Autowired MockMvc http;
    @Autowired ObjectMapper json;

    @Test
    void preservesProductionRoutesRequestsResponsesAndSchemas() throws Exception {
        var response = http.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Map<?, ?> actual = json.readValue(response, Map.class);
        try (var fixture = getClass().getResourceAsStream("/api-contract.json")) {
            Map<?, ?> expected = json.readValue(fixture, Map.class);
            assertThat(contract(actual.get("paths"))).isEqualTo(expected.get("paths"));
            assertThat(contract(actual.get("components"))).isEqualTo(expected.get("components"));
        }
    }

    private Object contract(Object value) {
        if (value instanceof Map<?, ?> fields) {
            Map<Object, Object> result = new LinkedHashMap<>();
            fields.forEach((key, child) -> {
                if (!DOCUMENTATION.contains(key)) result.put(key, contract(child));
            });
            return result;
        }
        if (value instanceof List<?> items) return items.stream().map(this::contract).toList();
        return value;
    }
}
