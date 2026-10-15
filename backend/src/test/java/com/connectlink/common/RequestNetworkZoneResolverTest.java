package com.connectlink.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RequestNetworkZoneResolverTest {

  @AfterEach
  void clear() {
    RequestContextHolder.resetRequestAttributes();
  }

  private static void request(String remoteAddr, String header) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(remoteAddr);
    if (header != null) {
      request.addHeader(RequestNetworkZoneResolver.HEADER, header);
    }
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  private static RequestNetworkZoneResolver resolver(
      List<String> cidrs, boolean headerOverride, NetworkZone defaultZone) {
    NetworkZoneProperties properties = new NetworkZoneProperties();
    properties.setInternalCidrs(cidrs);
    properties.setHeaderOverride(headerOverride);
    properties.setDefaultZone(defaultZone);
    return new RequestNetworkZoneResolver(properties);
  }

  @Test
  void 운영_설정은_내부_대역만_내부망이고_헤더는_무시한다() {
    RequestNetworkZoneResolver resolver =
        resolver(List.of("10.0.0.0/8"), false, NetworkZone.EXTERNAL);
    request("10.12.3.4", null);
    assertThat(resolver.current()).isEqualTo(NetworkZone.INTERNAL);
    request("203.0.113.9", "INTERNAL");
    assertThat(resolver.current()).isEqualTo(NetworkZone.EXTERNAL);
    assertThatThrownBy(() -> resolver.ensureInternal("엑셀 다운로드"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("내부망에서만")
        .extracting(e -> ((BusinessException) e).getCode())
        .isEqualTo(NetworkZoneResolver.INTERNAL_ONLY);
  }

  @Test
  void 시연_설정은_헤더로_망을_바꾸고_없으면_기본값_내부망이다() {
    RequestNetworkZoneResolver resolver = resolver(List.of(), true, NetworkZone.INTERNAL);
    request("203.0.113.9", null);
    assertThat(resolver.current()).isEqualTo(NetworkZone.INTERNAL);
    request("203.0.113.9", "external");
    assertThat(resolver.current()).isEqualTo(NetworkZone.EXTERNAL);
    request("203.0.113.9", "nonsense");
    assertThat(resolver.current()).isEqualTo(NetworkZone.INTERNAL);
  }

  @Test
  void 요청_밖에서는_기본값이다() {
    assertThat(resolver(List.of(), true, NetworkZone.EXTERNAL).current())
        .isEqualTo(NetworkZone.EXTERNAL);
  }
}
