// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("com.android.library") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
    id("com.google.firebase.firebase-perf") version "2.0.2" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

fun execShell(scriptPath: String): List<String> {
    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    return if (isWindows) {
        listOf("powershell", "-ExecutionPolicy", "Bypass", "-File", scriptPath)
    } else {
        listOf("pwsh", "-File", scriptPath)
    }
}

tasks.register<Exec>("logicAuditFast") {
    group = "verification"
    description = "Runs the fast AppOrganizer logic bug audit."
    workingDir = rootDir
    commandLine(execShell("${rootDir}/scripts/logic_audit_fast.ps1"))
}

tasks.register<Exec>("logicAuditSemantic") {
    group = "verification"
    description = "Runs the semantic AppOrganizer logic audit wrapper."
    workingDir = rootDir
    commandLine(execShell("${rootDir}/scripts/logic_audit_semantic.ps1"))
}

tasks.register<Exec>("logicAuditDeep") {
    group = "verification"
    description = "Runs the full section-by-section logic/code audit and stores detailed reports."
    workingDir = rootDir
    commandLine(execShell("${rootDir}/scripts/logic_audit_deep.ps1"))
}

tasks.register("qualityGate") {
    group = "verification"
    description = "Runs lint, unit tests, detekt, ktlint and the fast logic audit."
    dependsOn(":app:lintDebug", ":app:testDebugUnitTest", ":app:detekt", "logicAuditFast")
}
