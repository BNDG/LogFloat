plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("tech.medivh.plugin.publisher") version "1.2.1"
    `maven-publish`
    signing
}

// 从 gradle.properties 中加载发布信息
val groupId: String by project
val artifactId: String by project
val versionName: String by project

android {
    namespace = "com.bndg.floatlog"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "io.github.bndg"
                artifactId = "floatlog"
                version = "1.0.2"

                // 确保组件已初始化
                from(components["release"])
                pom{
                    name = "floatlog"
                    description = "android okhttp float log"
                    url = "https://github.com/BNDG/LogFloat"
                    licenses{
                        license{
                            name = "GPL-3.0 license"
                            url = "https://www.gnu.org/licenses/gpl-3.0.txt"
                        }
                    }
                    developers {
                        developer {
                            id = "id"
                            name = "name"
                            email = "email"
                        }
                    }
                    scm {
                        connection = "scm:git:"
                        url = "https://github.com/BNDG/LogFloat"
                    }
                }
            }
        }

        repositories {
            maven {
                name = "sonatype"
                url = uri(
                    if (version.toString().endsWith("SNAPSHOT")) {
                        "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                    } else {
                        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                    }
                )

                credentials {
                    username = System.getenv("SONATYPE_USERNAME") ?: ""
                    password = System.getenv("SONATYPE_PASSWORD") ?: ""
                }
            }
        }
    }

    signing {
        useGpgCmd()
        sign(publishing.publications["release"])
    }
}

medivhPublisher{
    groupId = "io.github.bndg"
    artifactId = "floatlog"
    version = "1.0.2"
    pom{
        name = "floatlog"
        description = "android okhttp float log"
        url = "https://github.com/BNDG/LogFloat"
        licenses{
            license{
                name = "GPL-3.0 license"
                url = "https://www.gnu.org/licenses/gpl-3.0.txt"
            }
        }
        developers {
            developer {
                id = "bndg"
                name = "bndg"
                email = "email"
            }
        }
        scm {
            connection = "scm:git:https://github.com/BNDG/LogFloat.git"
            url = "https://github.com/BNDG/LogFloat"
        }
    }
}