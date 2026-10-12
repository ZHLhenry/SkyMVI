import java.io.File
import java.io.FileInputStream
import java.util.Properties

val localProps = Properties().apply {
    val file = File(rootDir, "local.properties")
    if (file.exists()) load(FileInputStream(file))
}
val useLocalSkyBuildLogic = localProps.getProperty("useLocalSkyBuildLogic")?.toBooleanStrictOrNull() ?: false
val useLocalSkyWidgetCompose = localProps.getProperty("useLocalSkyWidgetCompose")?.toBooleanStrictOrNull() ?: false

pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/central")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }
        maven {
            credentials {
                username = "677b6d7687eb3ab8bcc7ac20"
                password = "Tqmt4VtWBTp)"
            }
            url = uri("https://packages.aliyun.com/6732fc8f356ccaf8531a1487/maven/skybuildlogic")
            content {
                // 插件标记（group 即插件 ID）+ com.sky.* 产物，避免无关插件探测私有库
                includeGroupByRegex("sky\\.android.*")
                includeGroupByRegex("com\\.sky.*")
            }
        }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/central")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        /** aliyun **/
        fun aliyunMaven(repoUrl: String) {
            maven {
                credentials {
                    username = "677b4e5b259532263f6b30a6"
                    password = "RnVrdxoghjKo"
                }
                url = uri(repoUrl)
                content {
                    // 私有库仅托管 Sky 系列产物：com.sky.lib（SkyMVI/SkyWidgetCompose）、com.sky.buildLogic（hilt-noop-processor）
                    includeGroupByRegex("com\\.sky.*")
                }
            }
        }
        aliyunMaven("https://packages.aliyun.com/6732fc8f356ccaf8531a1487/maven/skymvi")
        aliyunMaven("https://packages.aliyun.com/6732fc8f356ccaf8531a1487/maven/skybuildlogic")
        aliyunMaven("https://packages.aliyun.com/6732fc8f356ccaf8531a1487/maven/skywidgetcompose")
        maven("${rootDir}/build/repo")
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "SkyMVI"

if (useLocalSkyBuildLogic) {
    includeBuild("/Users/henry/workProject/androidProject/lib/SkyBuildLogic/buildLogicLib")
}
if (useLocalSkyWidgetCompose) {
    include(":SkyWidgetComposeLib")
    project(":SkyWidgetComposeLib").projectDir = file("../SkyWidgetCompose/SkyWidgetComposeLib")
}

include(":app")
include(":core:common")
include(":core:model")
include(":SkyMVILib")