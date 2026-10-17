package com.connectlink.opinion;

import com.connectlink.opinion.dto.OpinionCollationResponse;
import com.connectlink.opinion.dto.OpinionCollationResponse.Row;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 의견 취합표 엑셀 파일. "결과 요약", "질문별 답"(질문이 있을 때), "기관별 취합표" 이고, 화면의 회신 현황·취합표와 같은 값({@link
 * OpinionCollationResponse})을 쓴다.
 *
 * @param fileName 내려받을 이름(예: 의견취합_요청명_20261017.xlsx)
 */
public record OpinionExcel(String fileName, byte[] content) {

  public static final String CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  static OpinionExcel of(
      OpinionRequest request, OpinionCollationResponse collation, LocalDateTime now) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Styles styles = new Styles(workbook);
      writeSummary(workbook.createSheet("결과 요약"), styles, request, collation, now);
      if (!collation.questions().isEmpty()) {
        writeQuestions(workbook.createSheet("질문별 답"), styles, collation.questions());
      }
      writeRows(workbook.createSheet("기관별 취합표"), styles, collation);
      workbook.write(out);
      return new OpinionExcel(fileName(request, now), out.toByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException("엑셀 파일을 만들지 못했습니다.", e);
    }
  }

  private static void writeSummary(
      Sheet sheet,
      Styles styles,
      OpinionRequest request,
      OpinionCollationResponse collation,
      LocalDateTime now) {
    OpinionCollationResponse.Summary summary = collation.summary();
    String status = request.isCancelled() ? "마감(취소)" : request.getStatus().getLabel();
    List<String[]> lines =
        List.of(
            new String[] {"요청명", request.getTitle()},
            new String[] {"업무", request.getBusiness().getLabel()},
            new String[] {"상태", status},
            new String[] {"회신 기한", request.getDueDate().toString()},
            new String[] {"작성자", request.getOwnerName()},
            new String[] {"대상 기관", summary.targetCount() + "곳"},
            new String[] {"제출 기관", summary.repliedCount() + "곳"},
            new String[] {"미회신 기관", summary.notRepliedCount() + "곳"},
            new String[] {"회신율", summary.replyRate() + "%"},
            new String[] {"만든 일시", now.format(DATE_TIME)});
    int r = 0;
    for (String[] line : lines) {
      var row = sheet.createRow(r++);
      cell(row, 0, line[0], styles.header);
      cell(row, 1, line[1], styles.body);
    }
    var notReplied = sheet.createRow(r);
    cell(notReplied, 0, "미회신 기관 이름", styles.header);
    cell(notReplied, 1, String.join(", ", summary.notRepliedOrganizations()), styles.body);
    sheet.setColumnWidth(0, 16 * 256);
    sheet.setColumnWidth(1, 40 * 256);
    sheet.setColumnWidth(2, 60 * 256);
  }

  /** 질문마다 한 묶음. 객관식은 선택지별 기관 수와 기관, 주관식은 기관별 답이다. */
  private static void writeQuestions(
      Sheet sheet, Styles styles, List<OpinionCollationResponse.QuestionSummary> questions) {
    int r = 0;
    int n = 1;
    for (OpinionCollationResponse.QuestionSummary q : questions) {
      var title = sheet.createRow(r++);
      cell(title, 0, n++ + ". " + q.title(), styles.header);
      cell(
          title,
          1,
          q.typeLabel() + (q.required() ? " · 필수" : "") + " · 답한 기관 " + q.answeredCount() + "곳",
          styles.header);
      cell(title, 2, null, styles.header);
      if (q.type().isChoice()) {
        for (OpinionCollationResponse.OptionCount option : q.options()) {
          var row = sheet.createRow(r++);
          cell(row, 0, option.label(), styles.body);
          row.createCell(1).setCellValue(option.count());
          row.getCell(1).setCellStyle(styles.body);
          cell(row, 2, String.join(", ", option.organizations()), styles.body);
        }
      } else {
        for (OpinionCollationResponse.TextAnswer answer : q.textAnswers()) {
          var row = sheet.createRow(r++);
          cell(row, 0, answer.organizationName(), styles.body);
          cell(row, 1, answer.text(), styles.wrap);
          cell(row, 2, null, styles.body);
        }
      }
      r++;
    }
    sheet.setColumnWidth(0, 40 * 256);
    sheet.setColumnWidth(1, 60 * 256);
    sheet.setColumnWidth(2, 60 * 256);
  }

  private static void writeRows(Sheet sheet, Styles styles, OpinionCollationResponse collation) {
    List<String> headers = new ArrayList<>(List.of("순번", "기관", "회신 담당자", "회신 여부"));
    List<Integer> widths = new ArrayList<>(List.of(6, 18, 18, 10));
    int n = 1;
    for (OpinionCollationResponse.QuestionSummary q : collation.questions()) {
      headers.add(n++ + ". " + q.title());
      widths.add(q.type().isChoice() ? 20 : 40);
    }
    headers.addAll(List.of("작성자", "제출 일시", "상태"));
    widths.addAll(List.of(10, 18, 10));
    var head = sheet.createRow(0);
    for (int i = 0; i < headers.size(); i++) {
      cell(head, i, headers.get(i), styles.header);
      sheet.setColumnWidth(i, widths.get(i) * 256);
    }
    int r = 1;
    for (Row row : collation.rows()) {
      var line = sheet.createRow(r);
      line.createCell(0).setCellValue(r);
      line.getCell(0).setCellStyle(styles.body);
      cell(line, 1, row.organizationName(), styles.body);
      cell(line, 2, String.join(", ", row.recipientNames()), styles.body);
      cell(line, 3, row.replied() ? "회신" : "미회신", styles.body);
      int c = 4;
      for (OpinionCollationResponse.RowAnswer answer : row.answers()) {
        cell(line, c++, answer.value(), styles.wrap);
      }
      cell(line, c++, row.replied() ? row.replierName() : null, styles.body);
      cell(
          line,
          c++,
          row.submittedAt() == null ? null : row.submittedAt().format(DATE_TIME),
          styles.body);
      cell(line, c, row.replyStatusLabel() == null ? "미작성" : row.replyStatusLabel(), styles.body);
      r++;
    }
    sheet.createFreezePane(0, 1);
  }

  private static void cell(
      org.apache.poi.ss.usermodel.Row row, int column, String value, CellStyle style) {
    var cell = row.createCell(column);
    cell.setCellValue(value == null ? "" : value);
    cell.setCellStyle(style);
  }

  /** 파일 이름에 쓸 수 없는 글자를 밑줄로 바꾸고 요청명은 40자까지 쓴다. */
  private static String fileName(OpinionRequest request, LocalDateTime now) {
    String title = request.getTitle().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    if (title.length() > 40) {
      title = title.substring(0, 40);
    }
    return "의견취합_" + title + "_" + now.format(FILE_DATE) + ".xlsx";
  }

  private static final class Styles {
    final CellStyle header;
    final CellStyle body;
    final CellStyle wrap;

    Styles(XSSFWorkbook workbook) {
      Font bold = workbook.createFont();
      bold.setBold(true);
      header = bordered(workbook);
      header.setFont(bold);
      header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      body = bordered(workbook);
      wrap = bordered(workbook);
      wrap.setWrapText(true);
    }

    private static CellStyle bordered(XSSFWorkbook workbook) {
      CellStyle style = workbook.createCellStyle();
      style.setBorderTop(BorderStyle.THIN);
      style.setBorderBottom(BorderStyle.THIN);
      style.setBorderLeft(BorderStyle.THIN);
      style.setBorderRight(BorderStyle.THIN);
      style.setVerticalAlignment(VerticalAlignment.TOP);
      return style;
    }
  }
}
