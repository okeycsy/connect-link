import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("com.github.node-gradle.node") version "7.1.0"
}

// Node는 시스템(또는 CI의 setup-node)에 설치된 것을 쓴다. 빌드 중에 내려받지 않는다.
node {
    download.set(false)
}

val sourceFiles = fileTree("src") + fileTree(".") {
    include("index.html", "package.json", "package-lock.json", "vite.config.ts", "tsconfig*.json", "eslint.config.js", ".prettierrc")
}

tasks.register<NpmTask>("npmRunBuild") {
    description = "Vite 프로덕션 빌드. 결과는 dist/ 이며 backend의 app.jar에 포함된다."
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "build"))
    inputs.files(sourceFiles)
    outputs.dir("dist")
}

tasks.register<NpmTask>("npmRunLint") {
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "lint"))
    inputs.files(sourceFiles)
    outputs.upToDateWhen { true }
}

tasks.register<NpmTask>("npmRunTest") {
    dependsOn(tasks.npmInstall)
    args.set(listOf("test"))
    inputs.files(sourceFiles)
    outputs.upToDateWhen { true }
}

tasks.register("check") {
    dependsOn("npmRunLint", "npmRunTest")
}

tasks.register("build") {
    dependsOn("check", "npmRunBuild")
}

tasks.register<Delete>("clean") {
    delete("dist")
}
