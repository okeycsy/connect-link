package com.connectlink.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 화면 라우트(/account-info/home 같은 주소)로 바로 들어오거나 새로고침해도 React 앱이 뜨도록 index.html 로 넘긴다. API, OpenAPI 문서,
 * H2 콘솔, 확장자가 있는 정적 파일은 제외한다.
 */
@Controller
public class SpaForwardController {

  @GetMapping({
    "/",
    "/{path:^(?!api$|v3$|swagger-ui$|h2-console$|assets$)[^.]*}",
    "/{path:^(?!api$|v3$|swagger-ui$|h2-console$|assets$)[^.]*}/**"
  })
  public String forward() {
    return "forward:/index.html";
  }
}
