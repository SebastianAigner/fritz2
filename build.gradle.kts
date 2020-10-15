buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.4.0"))
    }
}

plugins {
}

repositories {
    jcenter() // or maven { url 'https://dl.bintray.com/kotlin/dokka' }
}

allprojects {
    //manage common setting and dependencies
}

subprojects {
    group = "dev.fritz2"
    version = "0.7.2"

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
    }
}