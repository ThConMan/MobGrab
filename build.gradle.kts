plugins {
    java
}

group = "com.mobgrab"
version = "2.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.12") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.0") { isTransitive = false }
    compileOnly("com.intellectualsites.plotsquared:plotsquared-core:7.5.11") { isTransitive = false }
    compileOnly("com.intellectualsites.plotsquared:plotsquared-bukkit:7.5.11") { isTransitive = false }
    compileOnly("com.github.TechFortress:GriefPrevention:17.0.0") { isTransitive = false }
    compileOnly("dev.rosewood:rosestacker:1.5.38") { isTransitive = false }
}

tasks.jar {
    destinationDirectory.set(file("/home/con/smp/plugins"))
    archiveFileName.set("MobGrab.jar")
}
