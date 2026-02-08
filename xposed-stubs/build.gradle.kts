plugins {
    `java-library`
}

// Compile on whatever JDK is installed (often 21 on dev machines),
// but target Java 17 bytecode to match the Android project baseline.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
