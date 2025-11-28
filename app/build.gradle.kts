plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("jacoco")
}

android {
    namespace = "com.example.englishforum"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.englishforum"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"https://englishforum-hvftgvb8dzendwe4.koreacentral-01.azurewebsites.net/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
    }
    
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
        }
    }
}
jacoco {
    toolVersion = "0.8.12"
}

// Configure all Test tasks with JaCoCo
tasks.withType<Test>().configureEach {
    configure<JacocoTaskExtension> {
        // Generate execution data
        isEnabled = true
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
    
    // Run with JaCoCo agent
    finalizedBy("jacocoTestReport")
}

// Main coverage report task
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generate Jacoco coverage reports for Debug build"
    
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$ViewInjector*.*",
        "**/*\$ViewBinder*.*",
        "**/Lambda$*.class",
        "**/Lambda.class",
        "**/*Lambda.class",
        "**/*Lambda*.class",
        "**/*_MembersInjector.class",
        "**/Dagger*Component*.*",
        "**/*Module_*Factory.class",
        "**/di/module/*",
        "**/*_Factory*.*",
        "**/*Module*.*",
        "**/*Dagger*.*",
        "**/*Hilt*.*"
    )

    val javaClasses = fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
        exclude(fileFilter)
    }
    val kotlinClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(fileFilter)
    }
    
    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    sourceDirectories.setFrom(files(
        "src/main/java",
        "src/main/kotlin"
    ))

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/*.exec",
                "outputs/code-coverage/connected/*coverage.ec"
            )
        }
    )
}

// Coverage verification task with rules
tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Verify Jacoco coverage metrics"
    
    dependsOn("jacocoTestReport")

    violationRules {
        rule {
            enabled = true
            element = "CLASS"
            
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.50".toBigDecimal()
            }
            
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.40".toBigDecimal()
            }
        }
        
        rule {
            enabled = true
            element = "PACKAGE"
            
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
        }
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$ViewInjector*.*",
        "**/*\$ViewBinder*.*",
        "**/Lambda$*.class",
        "**/Lambda.class",
        "**/*Lambda.class",
        "**/*Lambda*.class",
        "**/*_MembersInjector.class",
        "**/Dagger*Component*.*",
        "**/*Module_*Factory.class",
        "**/di/module/*",
        "**/*_Factory*.*",
        "**/*Module*.*",
        "**/*Dagger*.*",
        "**/*Hilt*.*"
    )

    val javaClasses = fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
        exclude(fileFilter)
    }
    val kotlinClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(fileFilter)
    }
    
    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    sourceDirectories.setFrom(files(
        "src/main/java",
        "src/main/kotlin"
    ))

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/*.exec"
            )
        }
    )
}

// Instrumented test coverage report task
tasks.register<JacocoReport>("jacocoAndroidTestReport") {
    group = "verification"
    description = "Generate Jacoco coverage reports for Android instrumented tests"
    
    dependsOn("createDebugCoverageReport")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/androidTest/jacocoAndroidTestReport.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/androidTest/html"))
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$ViewInjector*.*",
        "**/*\$ViewBinder*.*",
        "**/Lambda$*.class",
        "**/Lambda.class",
        "**/*Lambda.class",
        "**/*Lambda*.class",
        "**/*_MembersInjector.class",
        "**/Dagger*Component*.*",
        "**/*Module_*Factory.class",
        "**/di/module/*",
        "**/*_Factory*.*",
        "**/*Module*.*",
        "**/*Dagger*.*",
        "**/*Hilt*.*"
    )

    val javaClasses = fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
        exclude(fileFilter)
    }
    val kotlinClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(fileFilter)
    }
    
    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    sourceDirectories.setFrom(files(
        "src/main/java",
        "src/main/kotlin"
    ))

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
                "outputs/code-coverage/connected/**/*.ec"
            )
        }
    )
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofitMoshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.okhttpLogging)
    implementation(libs.moshi)
    implementation(libs.moshiKotlin)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation.layout)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
    androidTestImplementation(kotlin("test"))
    androidTestImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestUtil("androidx.test:orchestrator:1.4.2")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
