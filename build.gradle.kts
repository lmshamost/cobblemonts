import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.4.21"
}

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.
}

base {
	archivesName = providers.gradleProperty("archives_base_name").get()
}

loom {
	mods {
		register("cobblemon_ts") {
			sourceSet(sourceSets.main.get())
		}
	}
	runs {
		configureEach {
			vmArgs("--enable-native-access=ALL-UNNAMED")
		}
		named("client") {
			task {
				// GraalVM 22.3.0(Cobblemon showdown) 不兼容 JDK 25，客户端运行时改用 JDK 21
				setExecutable("C:/Program Files/Java/jdk-21/bin/java.exe")
			}
		}
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    mappings("net.fabricmc:yarn:${providers.gradleProperty("yarn_mappings").get()}:v2")
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

	// Cobblemon 编译+运行时依赖，Loom重映射(含Kotlin元数据)解决class_2960
	modCompileOnly(files("libs/cobblemon-fabric-1.8.4+1.21.1.jar"))
	modRuntimeOnly(files("libs/cobblemon-fabric-1.8.4+1.21.1.jar"))

	// Mod Menu 配置入口（仅编译，运行时由 run/mods 提供）
	modCompileOnly(files("libs/modmenu-11.0.3.jar"))

	// Cloth Config 通用配置界面（仅编译，运行时由 run/mods 提供）
	// 注意：不能用 modRuntimeOnly，否则 Loader 不会解包 JiJ 的 basic-math，导致 NoClassDefFoundError: me/shedaniel/math/hectangle
	modCompileOnly(files("libs/cloth-config-15.0.140-fabric.jar"))
	// basic-math 是 Cloth Config 的编译依赖（JiJ 嵌套），从 cloth-config jar 内 META-INF/jars 提取，
	// 仅编译用：startColorField 等 API 的签名引用 me.shedaniel.math.Color，运行时仍由 Loader 解包提供
	modCompileOnly(files("libs/basic-math-0.6.1.jar"))

	// Cobblemon showdown 依赖的嵌套库（Loom remap 会丢失 fabric.mod.json 的 jars 字段，
	// 导致 GraalVM/MongoDB 类加载不到，此处手动补齐运行时 classpath）
	runtimeOnly(files(
		"libs/nested_jars_temp/graal-sdk-22.3.0.jar",
		"libs/nested_jars_temp/js-22.3.0.jar",
		"libs/nested_jars_temp/truffle-api-22.3.0.jar",
		"libs/nested_jars_temp/regex-22.3.0.jar",
		"libs/nested_jars_temp/icu4j-71.1.jar",
		"libs/nested_jars_temp/mongodb-driver-core-4.10.2.jar",
		"libs/nested_jars_temp/mongodb-driver-sync-4.10.2.jar"
	))
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_21
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}