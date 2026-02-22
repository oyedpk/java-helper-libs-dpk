dependencies {
    api(project(":timing-annotations"))
    implementation(libs.aspectj.rt)
    implementation(libs.aspectj.weaver)
    implementation(libs.slf4j.api)
    implementation(libs.spring.expression)

    testImplementation(libs.spring.context)
    testImplementation(libs.spring.test)
    testRuntimeOnly(libs.slf4j.simple)
}
