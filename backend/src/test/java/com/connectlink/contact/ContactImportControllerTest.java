package com.connectlink.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.connectlink.organization.Business;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** 담당자 일괄 등록(엑셀): 양식 내려받기, 검사, 등록, 권한과 내부망. 담당자를 실제로 넣으므로 따로 쓰는 DB 다. */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:contact-import-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactImportControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ContactRepository contactRepository;

  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
  }

  /** 양식과 같은 머리글에 줄들을 채운 xlsx. */
  private static byte[] sheet(String[]... rows) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("담당자");
      Row head = sheet.createRow(0);
      for (int i = 0; i < ContactImportSheet.HEADERS.length; i++) {
        head.createCell(i).setCellValue(ContactImportSheet.HEADERS[i]);
      }
      for (int r = 0; r < rows.length; r++) {
        Row row = sheet.createRow(r + 1);
        for (int i = 0; i < rows[r].length; i++) {
          row.createCell(i).setCellValue(rows[r][i]);
        }
      }
      workbook.write(out);
      return out.toByteArray();
    }
  }

  private static String[] row(String code, String name, String email) {
    return new String[] {
      code, name, "부담당", "전산", "개발, 운영", "내 계좌 한눈에", "디지털금융부", "", "대리", "02-0000-1234", "", email
    };
  }

  private ResultActions upload(byte[] file, boolean dryRun, RequestPostProcessor user)
      throws Exception {
    return mockMvc.perform(
        multipart("/api/v1/contacts/imports")
            .file(new MockMultipartFile("file", "contacts.xlsx", ContactExcel.CONTENT_TYPE, file))
            .param("business", "account-info")
            .param("dryRun", String.valueOf(dryRun))
            .with(csrf())
            .with(user));
  }

  private long countByEmail(String email) {
    return contactRepository.findAll().stream()
        .filter(c -> c.getBusiness() == Business.ACCOUNT_INFO && c.getEmail().equals(email))
        .count();
  }

  @Test
  void 양식은_머리글과_적는_법_시트가_있다() throws Exception {
    byte[] file =
        mockMvc
            .perform(
                get("/api/v1/contacts/import-template")
                    .param("business", "account-info")
                    .with(hong()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file))) {
      assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("기관 코드");
      assertThat(workbook.getSheet("적는 법")).isNotNull();
    }
  }

  @Test
  void 검사만_하면_등록하지_않고_줄마다_결과를_준다() throws Exception {
    String email = "import.dry@kb.example.com";
    upload(sheet(row("004", "일괄검사", email)), true, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.createCount").value(1))
        .andExpect(jsonPath("$.saved").value(false))
        .andExpect(jsonPath("$.rows[0].rowNumber").value(2))
        .andExpect(jsonPath("$.rows[0].organizationName").value("KB국민은행"))
        .andExpect(jsonPath("$.rows[0].status").value("CREATE"));
    assertThat(countByEmail(email)).isZero();
  }

  @Test
  void 오류가_없으면_등록하고_이미_있는_이메일은_건너뛴다() throws Exception {
    String email = "import.ok@kb.example.com";
    upload(sheet(row("004", "일괄등록", email)), false, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saved").value(true))
        .andExpect(jsonPath("$.createCount").value(1));
    assertThat(countByEmail(email)).isEqualTo(1);

    upload(sheet(row("004", "일괄등록", email)), false, hong())
        .andExpect(jsonPath("$.skipCount").value(1))
        .andExpect(jsonPath("$.rows[0].status").value("SKIP"))
        .andExpect(jsonPath("$.saved").value(false));
    assertThat(countByEmail(email)).isEqualTo(1);
  }

  @Test
  void 한_줄이라도_틀리면_아무것도_등록하지_않는다() throws Exception {
    String good = "import.good@kb.example.com";
    String[] noItRole = row("004", "세부직무없음", "import.bad@kb.example.com");
    noItRole[4] = "";
    upload(
            sheet(
                row("004", "정상", good),
                noItRole,
                row("999", "없는기관", "import.none@kb.example.com"),
                row("004", "중복", good)),
            false,
            hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saved").value(false))
        .andExpect(jsonPath("$.errorCount").value(3))
        .andExpect(jsonPath("$.rows[1].status").value("ERROR"))
        .andExpect(jsonPath("$.rows[2].message").value("기관 코드 '999' 인 사용 중 기관이 없습니다."));
    assertThat(countByEmail(good)).isZero();
  }

  @Test
  void 머리글이_다르면_400_이다() throws Exception {
    upload("엑셀 아님".getBytes(), true, hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CONTACT_IMPORT_INVALID_FILE"));
  }

  @Test
  void 기관_담당자와_다른_업무_결제원은_일괄_등록하지_못한다() throws Exception {
    byte[] file = sheet(row("004", "권한", "import.denied@kb.example.com"));
    upload(file, true, TestUsers.ganaMember()).andExpect(status().isForbidden());
    upload(file, true, TestUsers.as(TestUsers.KFTC_CMS)).andExpect(status().isForbidden());
  }

  @Test
  void 인터넷망에서는_일괄_등록하지_못한다() throws Exception {
    mockMvc
        .perform(
            multipart("/api/v1/contacts/imports")
                .file(
                    new MockMultipartFile(
                        "file",
                        "c.xlsx",
                        ContactExcel.CONTENT_TYPE,
                        sheet(row("004", "망", "import.zone@kb.example.com"))))
                .param("business", "account-info")
                .header("X-Network-Zone", "EXTERNAL")
                .with(csrf())
                .with(hong()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INTERNAL_ONLY"));
  }
}
