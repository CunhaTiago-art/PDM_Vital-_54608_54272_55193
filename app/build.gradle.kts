plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "pt.ubi.di.pdm.vital"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pt.ubi.di.pdm.vital"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        encoding = "UTF-8"
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Room - base de dados local SQLite (Elemento 2)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // LiveData / ViewModel - observacao reativa das tabelas
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.recyclerview)

    // Firebase - so para o chat e as duvidas; os dados clinicos nao saem do aparelho
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}