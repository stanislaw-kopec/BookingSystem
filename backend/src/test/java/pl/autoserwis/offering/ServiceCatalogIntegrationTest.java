package pl.autoserwis.offering;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.PostgresTestConfiguration;
import pl.autoserwis.user.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class ServiceCatalogIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceCategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void anonymousAndClientCanReadGroupedCatalog() throws Exception {
        mvc.perform(get("/api/services"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[*].name", hasItems("Elektryka", "Mechanika", "Wulkanizacja")))
            .andExpect(jsonPath("$[*].services[*].name", hasItems("Wymiana opon", "Wymiana cewek", "Wymiana rozrządu")));
        mvc.perform(get("/api/services").with(user("client").roles("CLIENT")))
            .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me"))
            .andExpect(jsonPath("$.user").value(nullValue()));
    }

    @Test
    void anonymousCannotWriteEvenWithValidCsrf() throws Exception {
        mvc.perform(post("/api/service-categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(categoryJson("Nowa")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void clientCannotUseAnyCatalogMutation() throws Exception {
        for (String resource : new String[]{"services", "service-categories"}) {
            mvc.perform(post("/api/" + resource).with(user("client").roles("CLIENT")).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
            mvc.perform(put("/api/" + resource + "/1").with(user("client").roles("CLIENT")).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
            mvc.perform(delete("/api/" + resource + "/1").with(user("client").roles("CLIENT")).with(csrf()))
                .andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MECHANIC", "ADMIN"})
    void staffCanManageCategoriesAndMoveServices(String role) throws Exception {
        long first = createCategory("Przeglądy", role);
        long second = createCategory("Kontrole", role);
        MvcResult created = mvc.perform(post("/api/services").with(user("staff").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(serviceJson(first, "Kontrola hamulców")))
            .andExpect(status().isCreated()).andReturn();
        long serviceId = createdId(created);

        mvc.perform(put("/api/service-categories/" + first).with(user("staff").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(categoryJson("Przeglądy okresowe")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Przeglądy okresowe"));

        mvc.perform(delete("/api/service-categories/" + first).with(user("staff").roles(role)).with(csrf()))
            .andExpect(status().isConflict());

        mvc.perform(put("/api/services/" + serviceId).with(user("staff").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(serviceJson(second, "Kontrola układu hamulcowego")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.categoryId").value(second));
        mvc.perform(get("/api/service-categories/" + first))
            .andExpect(jsonPath("$.services", hasSize(0)));
        mvc.perform(get("/api/service-categories/" + second))
            .andExpect(jsonPath("$.services[0].name").value("Kontrola układu hamulcowego"));

        mvc.perform(delete("/api/service-categories/" + first).with(user("staff").roles(role)).with(csrf()))
            .andExpect(status().isNoContent());
        mvc.perform(delete("/api/services/" + serviceId).with(user("staff").roles(role)).with(csrf()))
            .andExpect(status().isNoContent());
        mvc.perform(delete("/api/service-categories/" + second).with(user("staff").roles(role)).with(csrf()))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/services/" + serviceId)).andExpect(status().isNotFound());
    }

    @Test
    void validatesNamesAndRejectsCaseInsensitiveDuplicatesAndMissingCategories() throws Exception {
        mvc.perform(post("/api/service-categories").with(user("staff").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(categoryJson("   ")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.name").isString());
        mvc.perform(post("/api/service-categories").with(user("staff").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(categoryJson("  ELEKTRYKA  ")))
            .andExpect(status().isConflict());
        long categoryId = categories.findAll().stream()
            .filter(category -> category.getName().equals("Wulkanizacja")).findFirst().orElseThrow().getId();
        mvc.perform(post("/api/services").with(user("staff").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(serviceJson(categoryId, "  WYMIANA OPON  ")))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/services").with(user("staff").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(serviceJson(Long.MAX_VALUE, "Nowa usługa")))
            .andExpect(status().isNotFound());
    }

    @Test
    void authenticatedWritesStillRequireCsrf() throws Exception {
        mvc.perform(post("/api/service-categories").with(user("staff").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(categoryJson("Bez tokena")))
            .andExpect(status().isForbidden());
    }

    @Test
    void loginUsesDatabasePasswordAndSessionWithFreshCsrfAfterAuthentication() throws Exception {
        users.saveAndFlush(new AppUser("test-mechanic", passwords.encode("test-password"), UserRole.MECHANIC));
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.token");
        String header = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.headerName");
        MockHttpSession anonymousSession = (MockHttpSession) csrfResult.getRequest().getSession(false);

        mvc.perform(post("/api/auth/login").session(anonymousSession).header(header, token)
                .param("username", "test-mechanic").param("password", "wrong-password"))
            .andExpect(status().isUnauthorized());

        MvcResult loggedIn = mvc.perform(post("/api/auth/login").session(anonymousSession).header(header, token)
                .param("username", "test-mechanic").param("password", "test-password"))
            .andExpect(status().isNoContent()).andReturn();
        MockHttpSession session = (MockHttpSession) loggedIn.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session))
            .andExpect(jsonPath("$.user.username").value("test-mechanic"))
            .andExpect(jsonPath("$.user.roles", hasItem("MECHANIC")));

        mvc.perform(post("/api/service-categories").session(session).header(header, token)
                .contentType(MediaType.APPLICATION_JSON).content(categoryJson("Stary token")))
            .andExpect(status().isForbidden());

        MvcResult newCsrf = mvc.perform(get("/api/auth/csrf").session(session)).andReturn();
        String newToken = JsonPath.read(newCsrf.getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/service-categories").session(session).header(header, newToken)
                .contentType(MediaType.APPLICATION_JSON).content(categoryJson("Nowy token")))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/logout").session(session).header(header, newToken))
            .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    private long createCategory(String name, String role) throws Exception {
        return createdId(mvc.perform(post("/api/service-categories").with(user("staff").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(categoryJson(name)))
            .andExpect(status().isCreated()).andReturn());
    }

    private long createdId(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private String categoryJson(String name) {
        return "{\"name\":\"" + name + "\",\"description\":\"Opis kategorii\"}";
    }

    private String serviceJson(long categoryId, String name) {
        return "{\"categoryId\":" + categoryId + ",\"name\":\"" + name + "\",\"description\":\"Opis usługi\"}";
    }
}
