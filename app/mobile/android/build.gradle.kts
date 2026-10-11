import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.PosixFilePermissions

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val experimentEnabled = providers.gradleProperty("imageExperiment").orNull == "true"
val newBuildDir: Directory = if (experimentEnabled) {
    val selected = providers.gradleProperty("imageExperimentOutputRoot").orNull
        ?: throw GradleException("EXPERIMENT_PRIVATE_OUTPUT_REQUIRED")
    val path = java.io.File(selected).toPath().toAbsolutePath().normalize()
    val repositoryRoot = rootDir.resolve("../../..").canonicalFile.toPath()
    require(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && !path.startsWith(repositoryRoot) &&
        path.toRealPath() == path && Files.getPosixFilePermissions(path) ==
        PosixFilePermissions.fromString("rwx------")) { "EXPERIMENT_PRIVATE_OUTPUT_REFUSED" }
    require(!Files.exists(path.resolve("gradle"))) { "EXPERIMENT_OUTPUT_NOT_EXCLUSIVE" }
    rootProject.layout.projectDirectory.dir(path.resolve("gradle").toString())
} else {
    rootProject.layout.buildDirectory.dir("../../build").get()
}
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
