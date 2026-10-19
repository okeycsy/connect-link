package com.connectlink.search;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 기관과_일시중단은_실제_데이터로_연락처는_샘플로_묶여_돌아온다() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/search").param("business", "cms").param("q", "다라").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.query").value("다라"))
        .andExpect(jsonPath("$.business").value("CMS"))
        .andExpect(jsonPath("$.groups[0].category").value("ORGANIZATION"))
        .andExpect(jsonPath("$.groups[0].label").value("기관"))
        .andExpect(jsonPath("$.groups[0].source").value("LIVE"))
        .andExpect(jsonPath("$.groups[0].hits", hasSize(1)))
        .andExpect(jsonPath("$.groups[0].hits[0].title").value("다라은행"))
        .andExpect(jsonPath("$.groups[0].hits[0].subtitle").value("참가기관 · 088"))
        .andExpect(
            jsonPath("$.groups[0].hits[0].link")
                .value(org.hamcrest.Matchers.startsWith("/cms/admin/organizations?org=")))
        .andExpect(jsonPath("$.groups[1].category").value("CONTACT"))
        .andExpect(jsonPath("$.groups[1].source").value("SAMPLE"))
        .andExpect(jsonPath("$.groups[1].hits[0].title").value("이다라"))
        .andExpect(jsonPath("$.groups[2].category").value("SUSPENSION"))
        .andExpect(jsonPath("$.groups[2].source").value("LIVE"))
        .andExpect(
            jsonPath("$.groups[2].hits[0].link")
                .value(org.hamcrest.Matchers.startsWith("/cms/suspensions/")));
  }

  @Test
  void 레일의_업무에_참여하지_않는_기관은_나오지_않는다() throws Exception {
    // 다라은행은 대출이동에 참여하지 않는다.
    mockMvc
        .perform(
            get("/api/v1/search")
                .param("business", "loan-transfer")
                .param("q", "다라은행")
                .with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groups[?(@.category=='ORGANIZATION')]").doesNotExist());
  }

  @Test
  void 빈_검색어와_모르는_업무() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/search").param("business", "cms").param("q", "  ").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groups", hasSize(0)));
    mockMvc
        .perform(
            get("/api/v1/search")
                .param("business", "nope")
                .param("q", "가나")
                .with(TestUsers.admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BUSINESS_NOT_FOUND"));
  }

  @Test
  void 로그인_없이는_401이다() throws Exception {
    mockMvc
        .perform(get("/api/v1/search").param("business", "cms").param("q", "가나"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void 담당_업무가_아닌_업무_영역은_검색할_수_없다() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/search")
                .param("business", "cms")
                .param("q", "가나")
                .with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }
}
