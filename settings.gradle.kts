rootProject.name = "another-one-chords-bot"

include("spring-boot-application")
include("chords-source-api")
include("am-dm-chords-source")
include("akkords-pro-chords-source")

pluginManagement {
    val dependencyManagement: String by settings
    val springframeworkBoot: String by settings
    val lombok: String by settings

    plugins {
        id("io.spring.dependency-management") version dependencyManagement
        id("org.springframework.boot") version springframeworkBoot
        id("io.freefair.lombok") version lombok
    }
}
