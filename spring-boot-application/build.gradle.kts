plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("com.github.kshashov:spring-boot-starter-telegram")
    implementation(project(":chords-source-api"))
    implementation(project(":am-dm-chords-source"))
    implementation(project(":akkords-pro-chords-source"))
}
