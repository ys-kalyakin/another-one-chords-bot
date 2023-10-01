import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.gradle.api.plugins.JavaPluginExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES

plugins {
    idea
    id("io.spring.dependency-management")
    id("io.freefair.lombok")
    id("org.springframework.boot")
}

idea {
    project {
        languageLevel = IdeaLanguageLevel(17)
    }
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

allprojects {
    group = "io.github.chords.bot"

    repositories {
        mavenLocal()
        mavenCentral()
    }

    apply(plugin = "io.spring.dependency-management")
    dependencyManagement {
        dependencies {
            imports {
                mavenBom(BOM_COORDINATES)
            }
            dependency("com.github.kshashov:spring-boot-starter-telegram:0.29")
            dependency("org.jsoup:jsoup:1.16.1")
        }
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging.showExceptions = true
        reports {
            junitXml.required.set(true)
            html.required.set(true)
        }
    }

    plugins.apply(JavaPlugin::class.java)
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    apply(plugin = "io.freefair.lombok")
}

