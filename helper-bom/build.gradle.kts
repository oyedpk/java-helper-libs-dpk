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
        api(project(":logging-annotations"))
        api(project(":logging-aop"))
        api(project(":resilience-annotations"))
        api(project(":resilience-aop"))
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
