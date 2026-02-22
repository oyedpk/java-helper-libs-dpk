dependencies {
    api(project(":resilience-annotations"))
    implementation(libs.aspectj.rt)
    implementation(libs.aspectj.weaver)
    implementation(libs.slf4j.api)

    testImplementation(libs.spring.context)
    testImplementation(libs.spring.test)
    testRuntimeOnly(libs.slf4j.simple)
}
