plugins {
    id("java")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "org.mardon"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val hibernateVersion = "7.4.5.Final"
    val postgresqlVersion = "42.7.13"
    val jacksonVersion = "2.22.2"

    val lombokVersion = "1.18.46"

    val assertjVersion = "3.27.7"

    val h2Version = "2.4.240"

    val testContainerVersion = "1.21.4"

    val ehcacheVersion = "3.12.0"

    val byteBuddyVersion = "1.18.14"

    runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")
    implementation("org.hibernate.orm:hibernate-core:$hibernateVersion")
    implementation("org.hibernate.orm:hibernate-envers:$hibernateVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    // Единая точка управления версиями для всей экосистемы Log4j
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.26.1"))
    // Мост SLF4J 2.x -> Log4j 2 (неявно скачает slf4j-api и log4j-api)
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl")
    // Ядро Log4j 2
    implementation("org.apache.logging.log4j:log4j-core")

    implementation("org.hibernate.orm:hibernate-jcache:$hibernateVersion")
    implementation("org.ehcache:ehcache:$ehcacheVersion:jakarta")

    // Подключение jakarta JAXB (API и реализация)
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.3")

    implementation("net.bytebuddy:byte-buddy:${byteBuddyVersion}")
// Требуется, если планируется изменение загруженных классов в рантайме:
    implementation("net.bytebuddy:byte-buddy-agent:${byteBuddyVersion}")

    // Основная реализация валидатора (версия 8.x использует пространство имен jakarta.*)
    implementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")

    // Реализация Expression Language (EL). Необходима для парсинга сообщений об ошибках
    implementation("org.glassfish.expressly:expressly:5.0.0")

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // Указываем классификатор jakarta для jpa
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")

    // Указываем классификатор jakarta для процессора
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")

    // Обязательная зависимость для процессора
    annotationProcessor("jakarta.annotation:jakarta.annotation-api:3.0.0")

    annotationProcessor("org.hibernate.orm:hibernate-processor:$hibernateVersion")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.assertj:assertj-core:$assertjVersion")

//    testImplementation("com.h2database:h2:$h2Version")
 
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")
}

// Направляем сгенерированные классы в нужную папку
tasks.withType<JavaCompile>().configureEach {
    options.generatedSourceOutputDirectory.set(
        layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main")
    )
}

// Указываем IDE, где искать Q-классы и метамодель
sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}