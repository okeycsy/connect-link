package com.connectlink.history;

import com.connectlink.history.dto.FieldChange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/** 변경 항목 목록을 JSON 문자열 한 컬럼에 넣고 꺼낸다. 항목 수가 대상마다 달라 별도 테이블보다 한 덩어리가 읽고 쓰기 쉽다. */
@Converter
public class FieldChangesConverter implements AttributeConverter<List<FieldChange>, String> {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<List<FieldChange>> TYPE = new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(List<FieldChange> changes) {
    try {
      return MAPPER.writeValueAsString(changes == null ? List.of() : changes);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("변경 항목을 JSON 으로 바꾸지 못했습니다.", e);
    }
  }

  @Override
  public List<FieldChange> convertToEntityAttribute(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return MAPPER.readValue(json, TYPE);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("변경 항목 JSON 을 읽지 못했습니다.", e);
    }
  }
}
