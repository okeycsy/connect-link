package com.connectlink.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void 기관_목록은_로그인_없이_조회되고_사용_중인_기관만_돌려준다() throws Exception {
    mockMvc
        .perform(get("/api/v1/organizations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(11)))
        .andExpect(jsonPath("$[0].code").value("KFTC"))
        .andExpect(jsonPath("$[0].typeLabel").value("결제원"))
        .andExpect(jsonPath("$[0].businesses", hasSize(4)));
  }

  @Test
  void 기관_목록은_가입_화면이_업무를_거르도록_참여_업무를_업무_순서로_담는다() throws Exception {
    mockMvc
        .perform(get("/api/v1/organizations"))
        .andExpect(jsonPath("$[?(@.code == '088')].businesses[*]").isArray())
        .andExpect(
            jsonPath(
                "$[?(@.code == '103')].businesses[*]",
                org.hamcrest.Matchers.contains("ACCOUNT_INFO", "LOAN_TRANSFER")));
  }

  @Test
  void 직무_목록은_로그인_없이_열리고_전산만_세부_직무가_있다() throws Exception {
    mockMvc
        .perform(get("/api/v1/job-roles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].code").value("BUSINESS"))
        .andExpect(jsonPath("$[0].label").value("현업"))
        .andExpect(jsonPath("$[0].itRoles", hasSize(0)))
        .andExpect(jsonPath("$[1].code").value("IT"))
        .andExpect(jsonPath("$[1].itRoles", hasSize(5)))
        .andExpect(jsonPath("$[1].itRoles[3].code").value("EXTERNAL_LINK"))
        .andExpect(jsonPath("$[1].itRoles[3].label").value("대외계"));
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
        .andExpect(jsonPath("$.totalElements").value(11))
        .andExpect(jsonPath("$.content[0].code").value("KFTC"))
        .andExpect(jsonPath("$.content[0].businesses", hasSize(4)))
        .andExpect(jsonPath("$.content[0].businesses[0].slug").value("account-info"))
        .andExpect(jsonPath("$.content[1].name").value("KB국민은행"))
        .andExpect(jsonPath("$.content[10].typeLabel").value("이용기관"));
  }

  @Test
  void 검색어_구분_업무_사용여부로_거른다() throws Exception {
    mockMvc
        .perform(get("/api/v1/organizations/search").param("keyword", "은행").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(5));
    mockMvc
        .perform(
            get("/api/v1/organizations/search").param("keyword", "004").with(TestUsers.admin()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].code").value("004"));
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
        .andExpect(jsonPath("$.content[*].active", everyItem(is(true))));
    // 시드에는 사용 중지된 기관이 없어 하나 만들어 끈다.
    String inactive = createInactive("OLDBANK", "옛은행");
    mockMvc
        .perform(
            get("/api/v1/organizations/search").param("active", "false").with(TestUsers.admin()))
        .andExpect(jsonPath("$.content[*].code", org.hamcrest.Matchers.hasItem(inactive)))
        .andExpect(jsonPath("$.content[*].active", everyItem(is(false))));
    mockMvc
        .perform(
            get("/api/v1/organizations/search").param("business", "nope").with(TestUsers.admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BUSINESS_NOT_FOUND"));
  }

  @Test
  void 상세는_이메일_도메인과_감사_정보를_담는다() throws Exception {
    Long id = organizationRepository.findByCode("088").orElseThrow().getId();
    mockMvc
        .perform(get("/api/v1/organizations/{id}", id).with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("신한은행"))
        .andExpect(jsonPath("$.emailDomain").value("shinhan.example.com"))
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
         "emailDomain":"NewB.example.com","businesses":["CMS","ACCOUNT_INFO"],"sector":"BANK"}
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
  void 참가기관을_업권_없이_등록하거나_업권을_지우면_400이다() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"NOSEC","name":"업권없는은행","type":"FINANCE_ORGANIZATION","businesses":["CMS"]}
                    """)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ORGANIZATION_SECTOR_REQUIRED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("sector"));

    Long id = organizationRepository.findByCode("240").orElseThrow().getId();
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"삼성증권","emailDomain":"samsung.example.com","active":true,"businesses":["ACCOUNT_INFO"],"sector":null}
                    """)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ORGANIZATION_SECTOR_REQUIRED"));
  }

  @Test
  void 이용기관은_업권_없이_등록된다() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"USEX","name":"가나상사","type":"USE_ORGANIZATION","businesses":["CMS"]}
                    """)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sector").doesNotExist());
  }

  @Test
  void 샘플_참가기관은_모두_업권이_있다() {
    assertThat(
            organizationRepository.findAll().stream()
                .filter(o -> o.getType() == OrganizationType.FINANCE_ORGANIZATION)
                .filter(o -> o.getSector() == null)
                .map(Organization::getName))
        .isEmpty();
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
    Long id = organizationRepository.findByCode("090").orElseThrow().getId();
    String body =
        """
        {"name":"카카오뱅크(변경)","emailDomain":"  ","active":false,"businesses":["LOAN_TRANSFER","CMS"],"sector":"BANK"}
        """;
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("카카오뱅크(변경)"))
        .andExpect(jsonPath("$.emailDomain").doesNotExist())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.businesses", hasSize(2)))
        .andExpect(jsonPath("$.businesses[1].code").value("CMS"))
        .andExpect(jsonPath("$.updatedBy").value("admin@kftc.example.com"));

    // 공개 목록에서는 사용 중지된 기관이 빠진다.
    mockMvc
        .perform(get("/api/v1/organizations"))
        .andExpect(jsonPath("$[*].code", not(org.hamcrest.Matchers.hasItem("090"))));
  }

  @Test
  void 업권은_참가기관만_두고_바꾸면_이력에_남는다() throws Exception {
    mockMvc
        .perform(get("/api/v1/finance-sectors").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("BANK"))
        .andExpect(jsonPath("$[0].label").value("은행"));

    Long id = organizationRepository.findByCode("240").orElseThrow().getId();
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"삼성증권","emailDomain":"samsung.example.com","active":true,"businesses":["ACCOUNT_INFO","AUTO_TRANSFER"],"sector":"INSURANCE"}
                    """)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sector").value("INSURANCE"))
        .andExpect(jsonPath("$.sectorLabel").value("보험사"));
    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "ORGANIZATION")
                .param("targetId", String.valueOf(id))
                .with(TestUsers.admin()))
        .andExpect(jsonPath("$.content[0].changes[0].label").value("업권"))
        .andExpect(jsonPath("$.content[0].changes[0].before").value("증권사"))
        .andExpect(jsonPath("$.content[0].changes[0].after").value("보험사"));

    // 결제원은 업권을 보내도 두지 않는다.
    Long kftc = organizationRepository.findByCode("KFTC").orElseThrow().getId();
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", kftc)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"금융결제원","emailDomain":"kftc.example.com","active":true,"businesses":["ACCOUNT_INFO","AUTO_TRANSFER","LOAN_TRANSFER","CMS"],"sector":"BANK"}
                    """)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sector").doesNotExist());
  }

  @Test
  void 참여_업무만_바꿔도_수정자와_수정_일시가_갱신된다() throws Exception {
    Long id = organizationRepository.findByCode("240").orElseThrow().getId();
    String body =
        """
        {"name":"삼성증권","emailDomain":"samsung.example.com","active":true,
         "businesses":["ACCOUNT_INFO","AUTO_TRANSFER","LOAN_TRANSFER"],"sector":"BANK"}
        """;
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.businesses", hasSize(3)))
        .andExpect(jsonPath("$.updatedBy").value("admin@kftc.example.com"));
  }

  @Test
  void 수정하면_바뀐_항목의_변경_전후가_이력에_남고_바뀐_것이_없으면_남지_않는다() throws Exception {
    Long id = organizationRepository.findByCode("004").orElseThrow().getId();
    String changed =
        """
        {"name":"KB국민은행(새 이름)","emailDomain":"kb.example.com","active":true,
         "businesses":["ACCOUNT_INFO","AUTO_TRANSFER","LOAN_TRANSFER"],"sector":"BANK"}
        """;
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(changed)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isOk());
    // 같은 값으로 한 번 더 저장: 이력이 늘지 않아야 한다.
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(changed)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "ORGANIZATION")
                .param("targetId", String.valueOf(id))
                .with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].action").value("UPDATED"))
        .andExpect(jsonPath("$.content[0].summary").value("KB국민은행(새 이름) 수정"))
        .andExpect(jsonPath("$.content[0].changedBy").value("admin@kftc.example.com"))
        .andExpect(jsonPath("$.content[0].changedByName").value("데모 관리자"))
        .andExpect(jsonPath("$.content[0].changes", hasSize(2)))
        .andExpect(jsonPath("$.content[0].changes[0].field").value("name"))
        .andExpect(jsonPath("$.content[0].changes[0].before").value("KB국민은행"))
        .andExpect(jsonPath("$.content[0].changes[0].after").value("KB국민은행(새 이름)"))
        .andExpect(jsonPath("$.content[0].changes[1].label").value("참여 업무"))
        .andExpect(jsonPath("$.content[0].changes[1].before").value("계좌통합관리, 자동이체통합관리, 대출이동, CMS"))
        .andExpect(jsonPath("$.content[0].changes[1].after").value("계좌통합관리, 자동이체통합관리, 대출이동"));
  }

  @Test
  void 등록하면_등록_이력이_남는다() throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/organizations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"code\":\"HIST\",\"name\":\"이력은행\",\"type\":\"FINANCE_ORGANIZATION\",\"businesses\":[\"CMS\"],\"sector\":\"BANK\"}")
                    .with(TestUsers.admin())
                    .with(csrf()))
            .andExpect(status().isCreated())
            .andReturn();
    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "ORGANIZATION")
                .param("targetId", String.valueOf(id))
                .with(TestUsers.admin()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].action").value("CREATED"))
        .andExpect(jsonPath("$.content[0].changes[0].after").value("HIST"))
        .andExpect(jsonPath("$.content[0].changes[4].after").value("은행"))
        .andExpect(jsonPath("$.content[0].changes[5].after").value("CMS"));
  }

  @Test
  void 관리자가_아니면_기준정보_관리_API는_403이다() throws Exception {
    Long id = organizationRepository.findByCode("004").orElseThrow().getId();
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

  /** 참가기관 하나를 만들고 사용 중지로 바꾼 뒤 그 코드를 돌려준다. */
  private String createInactive(String code, String name) throws Exception {
    String created =
        mockMvc
            .perform(
                post("/api/v1/organizations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"code":"%s","name":"%s","type":"FINANCE_ORGANIZATION","businesses":["CMS"],"sector":"BANK"}
                        """
                            .formatted(code, name))
                    .with(TestUsers.admin())
                    .with(csrf()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = com.jayway.jsonpath.JsonPath.parse(created).read("$.id", Long.class);
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"%s","active":false,"businesses":["CMS"],"sector":"BANK"}
                    """
                        .formatted(name))
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isOk());
    return code;
  }
}
