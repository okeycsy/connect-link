package com.connectlink.contact;

import com.connectlink.contact.dto.ContactSaveRequest;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 담당자 일괄 등록 엑셀. 양식 파일을 만들고({@link #template}), 채운 파일을 줄마다 등록 양식({@link ContactSaveRequest})으로
 * 읽는다({@link #read}). 첫 시트의 첫 줄은 머리글이고 둘째 줄부터 한 줄이 담당자 한 명이다. 기관은 3자리 기관 코드로 적는다.
 */
final class ContactImportSheet {

  /** 한 파일에 받는 최대 줄 수. */
  static final int MAX_ROWS = 1000;

  static final String[] HEADERS = {
    "기관 코드", "이름", "주/부담당", "직무", "전산 세부 직무", "세부업무", "부서", "팀", "직급", "내선번호", "휴대폰번호", "회사 이메일"
  };

  private static final String[] REQUIRED = {"기관 코드", "이름", "주/부담당", "직무", "부서", "내선번호", "회사 이메일"};

  private ContactImportSheet() {}

  /**
   * 엑셀에서 읽은 한 줄.
   *
   * @param rowNumber 엑셀의 줄 번호(머리글이 1)
   * @param organizationCode 적힌 기관 코드
   * @param request 등록 양식. 칸을 읽지 못했으면 null
   * @param error 칸을 읽지 못한 까닭. 읽었으면 null
   */
  record Parsed(int rowNumber, String organizationCode, ContactSaveRequest request, String error) {}

  /** 머리글과 예시 한 줄, 적는 법 시트가 있는 양식 파일. */
  static byte[] template(Business business) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(business.getLabel() + " 담당자");
      CellStyle header = workbook.createCellStyle();
      Font bold = workbook.createFont();
      bold.setBold(true);
      header.setFont(bold);
      header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      header.setBorderBottom(BorderStyle.THIN);
      Row head = sheet.createRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        head.createCell(i).setCellValue(HEADERS[i]);
        head.getCell(i).setCellStyle(header);
      }
      String[] example = {
        "004",
        "홍길동",
        "주담당",
        "전산",
        "개발, 운영",
        "내 계좌 한눈에",
        "디지털금융부",
        "계좌서비스팀",
        "과장",
        "1234",
        "010-0000-0000",
        "hong@kb.example.com"
      };
      Row row = sheet.createRow(1);
      for (int i = 0; i < example.length; i++) {
        row.createCell(i).setCellValue(example[i]);
      }
      for (int i = 0; i < HEADERS.length; i++) {
        sheet.setColumnWidth(i, 16 * 256);
      }
      sheet.createFreezePane(0, 1);

      Sheet guide = workbook.createSheet("적는 법");
      String[] lines = {
        "둘째 줄부터 한 줄에 담당자 한 명을 적습니다. 예시 줄(홍길동)은 지우고 올려 주세요.",
        "필수 칸: " + String.join(", ", REQUIRED),
        "기관 코드: 금융기관 공동코드 3자리(예: 004). 결제원은 KFTC.",
        "주/부담당: 주담당 또는 부담당",
        "직무: 현업 또는 전산",
        "전산 세부 직무: 직무가 전산이면 하나 이상, 쉼표로 나눕니다(개발, 운영, 인프라, 대외계, 상황실).",
        "휴대폰번호: 010-0000-0000 꼴(비워도 됩니다).",
        "같은 기관·업무에 같은 회사 이메일이 이미 있으면 그 줄은 건너뜁니다.",
        "한 줄이라도 틀리면 아무것도 등록하지 않습니다. 화면의 검사 결과를 보고 고쳐서 다시 올려 주세요.",
        "한 파일에 " + MAX_ROWS + "명까지 올립니다."
      };
      for (int i = 0; i < lines.length; i++) {
        guide.createRow(i).createCell(0).setCellValue(lines[i]);
      }
      guide.setColumnWidth(0, 100 * 256);
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("양식 파일을 만들지 못했습니다.", e);
    }
  }

  /**
   * 채운 파일을 읽는다. 빈 줄은 건너뛴다.
   *
   * @throws IllegalArgumentException 엑셀 파일이 아니거나 머리글이 양식과 다르거나 줄이 너무 많으면
   */
  static List<Parsed> read(InputStream in, Business business) {
    try (Workbook workbook = WorkbookFactory.create(in)) {
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter formatter = new DataFormatter();
      Row head = sheet.getRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        String value = head == null ? "" : text(formatter, head.getCell(i));
        if (!HEADERS[i].equals(value)) {
          throw new IllegalArgumentException(
              "머리글이 양식과 다릅니다(" + (i + 1) + "번째 칸은 '" + HEADERS[i] + "'). 양식 파일을 내려받아 채워 주세요.");
        }
      }
      List<Parsed> rows = new ArrayList<>();
      for (int r = 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        String[] cells = new String[HEADERS.length];
        boolean blank = true;
        for (int i = 0; i < HEADERS.length; i++) {
          cells[i] = text(formatter, row.getCell(i));
          blank &= cells[i].isEmpty();
        }
        if (blank) {
          continue;
        }
        if (rows.size() >= MAX_ROWS) {
          throw new IllegalArgumentException("한 파일에 " + MAX_ROWS + "명까지 올릴 수 있습니다.");
        }
        rows.add(parse(r + 1, cells, business));
      }
      return rows;
    } catch (IOException | RuntimeException e) {
      if (e instanceof IllegalArgumentException iae && iae.getMessage() != null) {
        throw iae;
      }
      throw new IllegalArgumentException("엑셀(xlsx) 파일을 읽지 못했습니다. 양식 파일을 내려받아 채워 주세요.", e);
    }
  }

  private static Parsed parse(int rowNumber, String[] c, Business business) {
    String code = c[0];
    List<String> missing = new ArrayList<>();
    for (String required : REQUIRED) {
      if (c[Arrays.asList(HEADERS).indexOf(required)].isEmpty()) {
        missing.add(required);
      }
    }
    if (!missing.isEmpty()) {
      return new Parsed(rowNumber, code, null, String.join(", ", missing) + " 칸이 비었습니다.");
    }
    ContactRank rank = labelOf(ContactRank.values(), c[2], ContactRank::getLabel);
    if (rank == null) {
      return new Parsed(rowNumber, code, null, "주/부담당은 '주담당' 또는 '부담당' 입니다.");
    }
    JobRole jobRole = labelOf(JobRole.values(), c[3], JobRole::getLabel);
    if (jobRole == null) {
      return new Parsed(rowNumber, code, null, "직무는 '현업' 또는 '전산' 입니다.");
    }
    List<ItRole> itRoles = new ArrayList<>();
    if (jobRole == JobRole.IT) {
      for (String label : c[4].split("[,，/]")) {
        if (label.isBlank()) {
          continue;
        }
        ItRole role = labelOf(ItRole.values(), label.trim(), ItRole::getLabel);
        if (role == null) {
          return new Parsed(
              rowNumber,
              code,
              null,
              "전산 세부 직무 '" + label.trim() + "' 를 모릅니다(개발, 운영, 인프라, 대외계, 상황실).");
        }
        itRoles.add(role);
      }
    }
    return new Parsed(
        rowNumber,
        code,
        new ContactSaveRequest(
            business, null, c[5], c[1], c[6], c[7], c[8], jobRole, itRoles, rank, c[9], c[10],
            c[11]),
        null);
  }

  /** "주담당" 이나 "주" 처럼 이름(앞부분)으로 고른다. */
  private static <E> E labelOf(
      E[] values, String text, java.util.function.Function<E, String> label) {
    String t = text.replace(" ", "");
    for (E value : values) {
      String l = label.apply(value);
      if (l.equals(t) || (t.length() == 1 && l.startsWith(t))) {
        return value;
      }
    }
    return null;
  }

  private static String text(DataFormatter formatter, Cell cell) {
    return cell == null ? "" : formatter.formatCellValue(cell).trim();
  }
}
