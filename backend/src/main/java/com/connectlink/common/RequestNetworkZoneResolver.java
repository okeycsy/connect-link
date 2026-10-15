package com.connectlink.common;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 현재 HTTP 요청으로 망을 판단한다. 순서: (설정이 허용하면) {@code X-Network-Zone} 헤더 → 내부 IP 대역 → 기본값. 요청 밖(예약 작업 등)에서는
 * 기본값이다.
 */
@Component
public class RequestNetworkZoneResolver implements NetworkZoneResolver {

  /** 화면의 망 전환 토글이 보내는 헤더. local·demo 프로필에서만 믿는다. */
  public static final String HEADER = "X-Network-Zone";

  private final NetworkZoneProperties properties;
  private final List<IpAddressMatcher> internalMatchers;

  public RequestNetworkZoneResolver(NetworkZoneProperties properties) {
    this.properties = properties;
    this.internalMatchers =
        properties.getInternalCidrs().stream().map(IpAddressMatcher::new).toList();
  }

  @Override
  public NetworkZone current() {
    HttpServletRequest request = currentRequest();
    if (request == null) {
      return properties.getDefaultZone();
    }
    if (properties.isHeaderOverride()) {
      String header = request.getHeader(HEADER);
      if (header != null && !header.isBlank()) {
        try {
          return NetworkZone.valueOf(header.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
          // 모르는 값은 무시하고 아래 판단으로 넘어간다.
        }
      }
    }
    String remote = request.getRemoteAddr();
    if (remote != null && internalMatchers.stream().anyMatch(m -> m.matches(remote))) {
      return NetworkZone.INTERNAL;
    }
    return properties.getDefaultZone();
  }

  private static HttpServletRequest currentRequest() {
    if (RequestContextHolder.getRequestAttributes()
        instanceof ServletRequestAttributes attributes) {
      return attributes.getRequest();
    }
    return null;
  }
}
