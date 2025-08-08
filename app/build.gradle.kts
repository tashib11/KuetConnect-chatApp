plugins {
    id("com.android.application")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.connectus"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.echonest"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures{
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
//    implementation ("com.github.USER:REPO:VERSION")
//    implementation ("com.github.USER:REPO:VERSION,aa")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-auth:23.1.0")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.firebase:firebase-storage:21.0.1")
    implementation("com.google.firebase:firebase-firestore:25.1.2")


//    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
////    implementation("com.google.firebase:firebase-auth:22.1.1")
//    implementation("com.google.firebase:firebase-auth")
//    implementation("com.google.firebase:firebase-database")
//    implementation("com.google.firebase:firebase-storage:20.2.1")
//    implementation("com.google.firebase:firebase-messaging:23.3.1")

    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    implementation ("com.github.bumptech.glide:glide:4.12.0")

//    implementation ("com.google.android.material:material:1.3.0-alpha02")
    implementation ("com.github.KwabenBerko:News-API-Java:1.0.2")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

implementation("com.airbnb.android:lottie:3.7.0")


    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.squareup.picasso:picasso:2.8")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation ("jp.wasabeef:glide-transformations:4.3.0")

    implementation("com.google.android.exoplayer:exoplayer:2.19.0")


}