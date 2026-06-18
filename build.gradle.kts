plugins {
    java
}

group = "com.mobgrab"
version = "2.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/") // Geyser/Floodgate
    maven("https://maven.enginehub.org/repo/") // WorldGuard/WorldEdit
    maven("https://jitpack.io") // GriefPrevention
    maven("https://repo.rosewooddev.io/repository/public/") // RoseStacker
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.21-alpha")
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.12") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.0") { isTransitive = false }
    compileOnly("com.intellectualsites.plotsquared:plotsquared-core:7.5.11") { isTransitive = false }
    compileOnly("com.intellectualsites.plotsquared:plotsquared-bukkit:7.5.11") { isTransitive = false }
    // PlotSquared API classes carry Guice type-annotations; JDK 25 javac needs TypeLiteral on the
    // classpath to read them even though we never call Guice. Provide it (compile-time only).
    compileOnly("com.google.inject:guice:5.1.0")
    compileOnly("com.github.TechFortress:GriefPrevention:17.0.0") { isTransitive = false }
    compileOnly("dev.rosewood:rosestacker:1.5.38") { isTransitive = false }

    testImplementation("io.papermc.paper:paper-api:26.2.build.21-alpha")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveFileName.set("MobGrab.jar")
    // Default output is build/libs/MobGrab.jar.
    // Override with -PpluginDir=/path/to/server/plugins (e.g. your SMP plugins folder).
    (project.findProperty("pluginDir") as String?)?.let {
        destinationDirectory.set(file(it))
    }
}
