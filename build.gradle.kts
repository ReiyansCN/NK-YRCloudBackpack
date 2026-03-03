plugins {
    id("java")
    id("maven-publish")
}

group = "com.yirankuma.yrcloudbackpack"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.lanink.cn/repository/maven-snapshots/") }
    maven { url = uri("https://repo.lanink.cn/repository/maven-releases/") }
}

dependencies {
    // Nukkit 核心依赖
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // 从 JitPack 引入 YRDatabase-api 依赖
    implementation("com.github.MufHead.YRDatabase:yrdatabase-nukkit:v2.0.0")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

tasks.jar {
    archiveBaseName.set("YRCloudBackpack")
    archiveVersion.set("")
    destinationDirectory.set(file("C:/迅雷下载"))

    doFirst {
        destinationDirectory.get().asFile.mkdirs()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "YRCloudBackpack"
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "LaninkRepo"
            // 根据版本是否包含 SNAPSHOT 来决定发布到哪个仓库
            url = if (version.toString().endsWith("SNAPSHOT")) {
                uri("https://repo.lanink.cn/repository/maven-snapshots/")
            } else {
                uri("https://repo.lanink.cn/repository/maven-releases/")
            }

            credentials {
                // 从 gradle.properties 或环境变量中读取凭证
                username = findProperty("repoUsername")?.toString() ?: System.getenv("REPO_USERNAME")
                password = findProperty("repoPassword")?.toString() ?: System.getenv("REPO_PASSWORD")
            }
        }
    }
}
