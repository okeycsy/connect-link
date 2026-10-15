package com.connectlink.organization;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OrganizationRepository organizationRepository;

  @Test
  void 기관_목록은_로그인_없이_조회되고_사용_중인_기관만_돌려준다() throws Exception {
    mockMvc
        .perform(get("/api/v1/organizations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(11)))
        .andExpect(jsonPath("$[0].code").value("KFTC"))
        .andExpect(jsonPath("$[0].typeLabel").value("결제원"))
        .andExpect(jsonPath("$[*].code", not(org.hamcrest.Matchers.hasItem("OLD"))));
  }

  @Test
  void 업무_영역은_네_개를_주소_slug와_함께_돌려준다() throws Exception {
    mockMvc
        .perform(get("/api/v1/businesses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)))
        .andExpect(jsonPath("$[0].code").value("ACCOUNT_INFO"))
        .andExpect(jsonPath("$[0].slug").value("account-info"))
        .andExpect(jsonPath("$[0].label").value("계좌통합관리"));
  }

  @Test
  void 관리자는_기준정보를_구분순_이름순으로_검색한다() throws Exception {
    mockMvc
        .perform(get("/api/v1/organizations/search").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(12))
        .andExpect(jsonPath("$.content[0].code").value("KFTC"))
        .andExpect(jsonPath("$.content[0].businesses", hasSize(4)))
        .andExpect(jsonPath("$.content[0].businesses[0].slug").value("account-info"))
        .andExpect(jsonPath("$.content[1].name").value("가나은행"))
        .andExpect(jsonPath("$.content[11].typeLabel").value("이용기관"));
  }

  @Test
  void 검색어_구분_업무_사용여부로_거른다() throws Exception {
    mockMvc
        .perform(get("/api/v1/organizations/search").param("keyword", "은행").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(5));
    mockMvc
        .perform(
            get("/api/v1/organizations/search").param("keyword", "gana").with(TestUsers.admin()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].code").value("GANA"));
    mockMvc
        .perform(
            get("/api/v1/organizations/search")
                .param("type", "USE_ORGANIZATION")
                .with(TestUsers.admin()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].code").value("HAHA"));
    mockMvc
        .perform(
            get("/api/v1/organizations/search")
                .param("business", "loan-transfer")
                .param("active", "true")
                .with(TestUsers.admin()))
        .andExpect(jsonPath("$.totalElements").value(5))
        .andExpect(jsonPath("$.content[*].code", not(org.hamcrest.Matchers.hasItem("OLD"))));
    mockMvc
        .perform(
            get("/api/v1/organizations/search").param("active", "false").with(TestUsers.admin()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].code").value("OLD"));
    mockMvc
        .perform(
            get("/api/v1/organizations/search").param("business", "nope").with(TestUsers.admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BUSINESS_NOT_FOUND"));
  }

  @Test
  void 상세는_이메일_도메인과_감사_정보를_담는다() throws Exception {
    Long id = organizationRepository.findByCode("DARA").orElseThrow().getId();
    mockMvc
        .perform(get("/api/v1/organizations/{id}", id).with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("다라은행"))
        .andExpect(jsonPath("$.emailDomain").value("dara.example.com"))
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.businesses", hasSize(3)))
        .andExpect(jsonPath("$.createdBy").value("seed"))
        .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    mockMvc
        .perform(get("/api/v1/organizations/{id}", 999_999).with(TestUsers.admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
  }

  @Test
  void 기관을_등록하면_참여_업무까지_저장되고_같은_코드는_409다() throws Exception {
    String body =
        """
        {"code":"NEWB","name":"새로은행","type":"FINANCE_ORGANIZATION",
         "emailDomain":"NewB.example.com","businesses":["CMS","ACCOUNT_INFO"]}
        """;
    mockMvc
        .perform(
            post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.emailDomain").value("newb.example.com"))
        .andExpect(jsonPath("$.businesses", hasSize(2)))
        .andExpect(jsonPath("$.businesses[0].code").value("ACCOUNT_INFO"))
        .andExpect(jsonPath("$.createdBy").value("admin@kftc.example.com"));

    mockMvc
        .perform(
            post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ORGANIZATION_CODE_DUPLICATE"));
  }

  @Test
  void 잘못된_입력은_필드별_메시지와_함께_400이다() throws Exception {
    String body =
        """
        {"code":"new b","name":"","type":"FINANCE_ORGANIZATION","emailDomain":"not a domain","businesses":[]}
        """;
    mockMvc
        .perform(
            post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(
            jsonPath("$.fieldErrors[?(@.field=='code')].message")
                .value("기관 코드는 영문 대문자·숫자·밑줄 2~20자입니다."))
        .andExpect(jsonPath("$.fieldErrors[?(@.field=='name')].message").value("기관명을 입력해 주세요."))
        .andExpect(
            jsonPath("$.fieldErrors[?(@.field=='emailDomain')].message")
                .value("이메일 도메인은 example.com 같은 형식입니다."));
  }

  @Test
  void 기관을_수정하면_참여_업무가_통째로_바뀌고_빈_도메인은_null이_된다() throws Exception {
    Long id = organizationRepository.findByCode("PAHA").orElseThrow().getId();
    String body =
        """
        {"name":"파하캐피탈(변경)","emailDomain":"  ","active":false,"businesses":["LOAN_TRANSFER","CMS"]}
        """;
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("파하캐피탈(변경)"))
        .andExpect(jsonPath("$.emailDomain").doesNotExist())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.businesses", hasSize(2)))
        .andExpect(jsonPath("$.businesses[1].code").value("CMS"));

    // 공개 목록에서는 사용 중지된 기관이 빠진다.
    mockMvc
        .perform(get("/api/v1/organizations"))
        .andExpect(jsonPath("$[*].code", not(org.hamcrest.Matchers.hasItem("PAHA"))));
  }

  @Test
  void 관리자가_아니면_기준정보_관리_API는_403이다() throws Exception {
    Long id = organizationRepository.findByCode("GANA").orElseThrow().getId();
    mockMvc
        .perform(get("/api/v1/organizations/search").with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    mockMvc
        .perform(get("/api/v1/organizations/{id}", id).with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\"XX\",\"name\":\"x\",\"type\":\"USE_ORGANIZATION\",\"businesses\":[]}")
                .with(TestUsers.ganaMember())
                .with(csrf()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\",\"active\":true,\"businesses\":[]}")
                .with(TestUsers.ganaMember())
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void 로그인_없는_등록_요청은_401이다() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
        .andExpect(status().isUnauthorized());
  }
}
