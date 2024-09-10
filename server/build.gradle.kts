plugins {
    id("java-library")
    id("kotlin")
    alias(libs.plugins.jetbrainsKotlinJvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(kotlin("stdlib"))
    // 回退到 2.0.0 可以避免在 Server模块的弃用警告，在最新依赖中没有支持从旧版本到新版本的直接映射（参考了stackoverflow上的回答）
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    testImplementation(libs.junit)
}