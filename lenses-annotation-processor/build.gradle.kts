plugins {
    kotlin("jvm")
//    kotlin("kapt")
    id("maven-publish")
}

val kotlinpoet_version = "1.6.0"
//val incap_version = "0.3"

kotlin {
    dependencies {
        api(kotlin("stdlib"))
        api(project(":core"))
        api(kotlin("stdlib-jdk8"))
        api("com.squareup:kotlinpoet:$kotlinpoet_version")
        api("com.squareup:kotlinpoet-classinspector-elements:$kotlinpoet_version")
        api("com.squareup:kotlinpoet-metadata:$kotlinpoet_version")
        api("com.squareup:kotlinpoet-metadata-specs:$kotlinpoet_version")
//                    compileOnly("net.ltgt.gradle.incap:incap:${incap_version}")
//                    configurations.get("kapt").dependencies.add(compileOnly("net.ltgt.gradle.incap:incap-processor:${incap_version}"))
//                    implementation(kotlin("compiler-embeddable"))
    }
}

publishing {
    repositories {
        maven {
            name = "bintray"
            val releaseUrl = "https://api.bintray.com/maven/jwstegemann/fritz2/${project.name}/;" +
                    "publish=0;" + // Never auto-publish to allow override.
                    "override=1"
            val snapshotUrl = "https://oss.jfrog.org/artifactory/oss-snapshot-local"
            val isRelease = System.getenv("GITHUB_EVENT_NAME").equals("release", true)

            url = uri(if (isRelease && !version.toString().endsWith("SNAPSHOT")) releaseUrl else snapshotUrl)

            credentials {
                username = "jwstegemann"
                password = System.getenv("BINTRAY_API_KEY")
            }
        }
    }
}
