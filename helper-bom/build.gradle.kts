plugins {
    `java-platform`
    `maven-publish`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform(rootProject.libs.junit.bom))

    constraints {
        api(project(":timing-annotations"))
        api(project(":timing-aop"))
        api(rootProject.libs.aspectj.rt)
        api(rootProject.libs.aspectj.weaver)
        api(rootProject.libs.spring.expression)
        api(rootProject.libs.slf4j.api)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenBom") {
            from(components["javaPlatform"])
        }
    }
}
