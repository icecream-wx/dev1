package com.example.employee;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private String json(String name, String email, String department, String position) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "email", email,
                "department", department,
                "position", position));
    }

    @Test
    void listReturnsSeededEmployees() throws Exception {
        // Tests share one Spring context and one in-memory database, so assert on
        // the seeded rows instead of an exact total count.
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].id", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].name", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].email", everyItem(notNullValue())))
                .andExpect(jsonPath("$[?(@.email == 'zhang.wei@example.com')].name", is(java.util.List.of("张伟"))))
                .andExpect(jsonPath("$[?(@.email == 'li.na@example.com')].name", is(java.util.List.of("李娜"))));
    }

    @Test
    void createReturns201AndPersistsEmployee() throws Exception {
        String email = uniqueEmail("create");

        String body = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("王五", email, "研发部", "前端工程师")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("王五")))
                .andExpect(jsonPath("$.email", is(email)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(body);

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + created.get("id").asLong() + ")].email", is(java.util.List.of(email))));
    }

    @Test
    void updateReturns200AndChangesFields() throws Exception {
        String email = uniqueEmail("update");
        String body = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("旧名字", email, "旧部门", "旧职位")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(put("/api/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("新名字", email, "新部门", "新职位")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.name", is("新名字")))
                .andExpect(jsonPath("$.department", is("新部门")))
                .andExpect(jsonPath("$.position", is("新职位")));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].name", is(java.util.List.of("新名字"))))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].department", is(java.util.List.of("新部门"))));
    }

    @Test
    void createRejectsDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("重复邮箱", "zhang.wei@example.com", "研发部", "工程师")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void updateWithOwnEmailIsAllowed() throws Exception {
        String email = uniqueEmail("self");
        String body = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("本人", email, "研发部", "工程师")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(put("/api/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("本人改名", email, "研发部", "高级工程师")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("本人改名")))
                .andExpect(jsonPath("$.position", is("高级工程师")));
    }

    @Test
    void updateRejectsEmailUsedByAnotherEmployee() throws Exception {
        String first = uniqueEmail("first");
        String body = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("员工一", first, "研发部", "工程师")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(put("/api/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("员工一", "li.na@example.com", "研发部", "工程师")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("   ", uniqueEmail("blank"), "研发部", "工程师")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void createRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("格式错误", "not-an-email", "研发部", "工程师")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void updateUnknownIdReturns404() throws Exception {
        mockMvc.perform(put("/api/employees/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("不存在", uniqueEmail("missing"), "研发部", "工程师")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/api/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
