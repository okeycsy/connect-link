plugins {
    java
    id("org.springframework.boot") version "2.7.18"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.2.1"
}

repositories {
    mavenCentral()
}

// 내부망 서버가 Java 17이다. 빌드 JDK는 17 이상 아무거나 쓰되 컴파일은 항상 Java 17 API에 맞춘다.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // 실제 메일 발송(내부 SMTP). 발송은 app.notification.delivery.enabled 가 켜진 서버에서만 한다. SHY 승인(2026-10-25).
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.flywaydb:flyway-core")
    implementation("org.springdoc:springdoc-openapi-ui:1.8.0")
    // 엑셀(xlsx) 생성. Spring Boot 가 버전을 관리하지 않아 직접 적는다. 5.2.x 는 Java 8 이상에서 돈다.
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

val openApiExport = layout.buildDirectory.file("openapi.json")

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("file.encoding", "UTF-8")
    // OpenApiExportTest가 프론트 타입 생성용 문서를 여기에 쓴다.
    systemProperty("openapi.export.path", openApiExport.get().asFile.path)
    // 빌드 캐시에서 테스트 결과를 복원할 때(CI 등) 이 파일도 함께 복원되도록 출력으로 선언한다.
    // 선언하지 않으면 캐시 적중 시 테스트가 실행되지 않아 파일이 없고 gen:api 가 실패한다.
    outputs.file(openApiExport)
}

spotless {
    java {
        googleJavaFormat("1.19.2")
        target("src/**/*.java")
    }
}

// 프론트 빌드 결과(frontend/dist)를 app.jar의 정적 자원으로 넣는다.
// 서버 테스트(:backend:test)는 프론트 빌드 없이 돌아간다.
val frontendDist = project(":frontend").layout.projectDirectory.dir("dist")

tasks.bootJar {
    archiveFileName.set("app.jar")
    dependsOn(":frontend:npmRunBuild")
    from(frontendDist) {
        into("BOOT-INF/classes/static")
    }
}

tasks.jar {
    // plain jar는 쓰지 않는다. app.jar 하나만 남긴다.
    enabled = false
}
