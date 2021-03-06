import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask

description = "Kotlin \"main\" script definition"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val jarBaseName = property("archivesBaseName") as String

val proguardLibraryJars by configurations.creating
val relocatedJarContents by configurations.creating
val embedded by configurations

dependencies {
    compileOnly("org.apache.ivy:ivy:2.5.0")
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-scripting-jvm-host"))
    compileOnly(project(":kotlin-scripting-dependencies"))
    compileOnly(project(":kotlin-script-util"))
    runtime(project(":kotlin-compiler-embeddable"))
    runtime(project(":kotlin-scripting-compiler-embeddable"))
    runtime(project(":kotlin-scripting-jvm-host-embeddable"))
    runtime(project(":kotlin-reflect"))
    embedded(project(":kotlin-scripting-common")) { isTransitive = false }
    embedded(project(":kotlin-scripting-jvm")) { isTransitive = false }
    embedded(project(":kotlin-scripting-jvm-host")) { isTransitive = false }
    embedded(project(":kotlin-scripting-dependencies")) { isTransitive = false }
    embedded(project(":kotlin-script-util")) { isTransitive = false }
    embedded("org.apache.ivy:ivy:2.5.0")
    embedded(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    proguardLibraryJars(files(firstFromJavaHomeThatExists("jre/lib/rt.jar", "../Classes/classes.jar"),
                              firstFromJavaHomeThatExists("jre/lib/jsse.jar", "../Classes/jsse.jar"),
                              toolsJarFile()))
    proguardLibraryJars(kotlinStdlib())
    proguardLibraryJars(project(":kotlin-reflect"))
    proguardLibraryJars(project(":kotlin-compiler"))

    relocatedJarContents(embedded)
    relocatedJarContents(mainSourceSet.output)
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

publish()

noDefaultJar()

val relocatedJar by task<ShadowJar> {
    configurations = listOf(relocatedJarContents)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(File(buildDir, "libs"))
    archiveClassifier.set("before-proguard")

    // don't add this files to resources classpath to avoid IDE exceptions on kotlin project
    from("jar-resources")

    if (kotlinBuildProperties.relocation) {
        packagesToRelocate.forEach {
            relocate(it, "$kotlinEmbeddableRootPackage.$it")
        }
    }
}

val proguard by task<ProGuardTask> {
    dependsOn(relocatedJar)
    configuration("main-kts.pro")

    injars(mapOf("filter" to "!META-INF/versions/**"), relocatedJar.get().outputs.files)

    val outputJar = fileFrom(buildDir, "libs", "$jarBaseName-$version-after-proguard.jar")

    outjars(outputJar)

    inputs.files(relocatedJar.get().outputs.files.singleFile)
    outputs.file(outputJar)

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraryJars)
}

val resultJar by task<Jar> {
    val pack = if (kotlinBuildProperties.proguard) proguard else relocatedJar
    dependsOn(pack)
    setupPublicJar(jarBaseName)
    from {
        zipTree(pack.get().outputs.files.singleFile)
    }
}

addArtifact("runtime", resultJar)
addArtifact("archives", resultJar)

sourcesJar()

javadocJar()
