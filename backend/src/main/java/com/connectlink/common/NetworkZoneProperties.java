package com.connectlink.common;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 망 판단 설정 ({@code app.network.*}).
 *
 * <ul>
 *   <li>{@code internal-cidrs}: 이 대역에서 온 요청은 내부망이다(운영에서 설정).
 *   <li>{@code header-override}: 켜면 요청 헤더 {@code X-Network-Zone} 값을 믿는다. 화면의 망 전환 토글용이라 local·demo
 *       에서만 켠다.
 *   <li>{@code default-zone}: 대역에도 헤더에도 해당하지 않을 때의 망. 운영은 EXTERNAL, 시연은 INTERNAL.
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.network")
public class NetworkZoneProperties {

  private List<String> internalCidrs = new ArrayList<>();
  private boolean headerOverride = false;
  private NetworkZone defaultZone = NetworkZone.EXTERNAL;

  public List<String> getInternalCidrs() {
    return internalCidrs;
  }

  public void setInternalCidrs(List<String> internalCidrs) {
    this.internalCidrs = internalCidrs;
  }

  public boolean isHeaderOverride() {
    return headerOverride;
  }

  public void setHeaderOverride(boolean headerOverride) {
    this.headerOverride = headerOverride;
  }

  public NetworkZone getDefaultZone() {
    return defaultZone;
  }

  public void setDefaultZone(NetworkZone defaultZone) {
    this.defaultZone = defaultZone;
  }
}
