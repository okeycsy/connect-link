// 루트 프로젝트는 하위 프로젝트(backend, frontend)를 묶기만 한다.
// `./gradlew build` 한 번으로 서버 테스트, 프론트 린트·테스트, app.jar 생성까지 끝난다.

allprojects {
    group = "com.connectlink"
    version = "0.1.0-SNAPSHOT"
}
