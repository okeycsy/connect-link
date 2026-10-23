package com.connectlink.contact;

import com.connectlink.organization.Business;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 담당자 목록 엑셀 파일(시트 하나). 화면 목록과 같은 범위·조건·순서다. 내부망 전용이라 휴대폰번호를 가리지 않는다.
 *
 * @param fileName 내려받을 이름(예: 담당자연락처_계좌통합관리_20261019.xlsx)
 */
public record ContactExcel(String fileName, byte[] content) {

  public static final String CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  private static final String[] HEADERS = {
    "기관", "기관 코드", "이름", "주/부담당", "직무", "세부업무", "부서", "팀", "직급", "내선번호", "휴대폰번호", "회사 이메일",
    "최근 확인일", "확인 필요", "연락 불가"
  };

  static ContactExcel of(
      Business business, List<Contact> contacts, LocalDateTime now, LocalDateTime threshold) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(business.getLabel() + " 담당자");
      CellStyle header = bordered(workbook);
      Font bold = workbook.createFont();
      bold.setBold(true);
      header.setFont(bold);
      header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      CellStyle body = bordered(workbook);

      Row head = sheet.createRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        head.createCell(i).setCellValue(HEADERS[i]);
        head.getCell(i).setCellStyle(header);
      }
      int r = 1;
      for (Contact c : contacts) {
        Row row = sheet.createRow(r++);
        String[] values = {
          c.getOrganization().getName(),
          c.getOrganization().getCode(),
          c.getName(),
          c.getRank().getLabel(),
          c.jobRoleLabel(),
          orEmpty(c.getSubBusiness()),
          c.getDepartment(),
          orEmpty(c.getTeam()),
          orEmpty(c.getJobTitle()),
          c.getExtensionPhone(),
          orEmpty(c.getMobilePhone()),
          c.getEmail(),
          c.getLastConfirmedAt().format(DATE),
          c.getLastConfirmedAt().isBefore(threshold) ? "6개월 경과" : "",
          unreachableOf(c)
        };
        for (int i = 0; i < values.length; i++) {
          row.createCell(i).setCellValue(values[i]);
          row.getCell(i).setCellStyle(body);
        }
      }
      for (int i = 0; i < HEADERS.length; i++) {
        sheet.autoSizeColumn(i);
      }
      sheet.createFreezePane(0, 1);
      workbook.write(out);
      return new ContactExcel(
          "담당자연락처_" + business.getLabel() + "_" + now.format(FILE_DATE) + ".xlsx",
          out.toByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException("엑셀 파일을 만들지 못했습니다.", e);
    }
  }

  /** 연락 불가 표시. "2026-10-24 메모" 처럼 표시한 날과 메모, 없으면 빈 칸. */
  private static String unreachableOf(Contact c) {
    if (!c.isUnreachable()) {
      return "";
    }
    String date = c.getUnreachableAt().format(DATE);
    return c.getUnreachableNote() == null ? date : date + " " + c.getUnreachableNote();
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  private static CellStyle bordered(XSSFWorkbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }
}
