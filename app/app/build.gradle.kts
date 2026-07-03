plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("therouter")
}

android {
    namespace = "com.example.sheeps"
    compileSdk = 37
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val fileName = "sheeps_${variant.versionName}.apk"
                output.outputFileName = fileName
            }
    }
    signingConfigs {
        create("release") {
            // 默认寻址路径相对于当前 app 模块目录
            storeFile = file("../sheeps.jks")
            storePassword = "sheeps123"
            keyAlias = "sheeps"
            keyPassword = "sheeps123"
        }
    }
    defaultConfig {
        applicationId = "com.example.sheeps"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "1.0.7"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

kapt {
    correctErrorTypes = true
}

dependencies {
    // Modular features dependencies
    implementation(project(":core"))
    implementation(project(":feature_splash"))
    implementation(project(":feature_menu"))
    implementation(project(":feature_game"))
    implementation(project(":feature_leaderboard"))

    // TheRouter Annotation Compiler for shell module
    kapt(libs.therouter.apt)
    ksp(libs.hilt.compiler)

    // Hilt must be declared directly in app module (not just via :core api)
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)

    // Compose Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}


// 定义一个干净的、可序列化的自定义 Task 类
abstract class SafeDeleteImagesTask : DefaultTask() {

    // 通过 Property 延迟获取项目根目录，避免在执行期直接调用 project
    @get:InputDirectory
    abstract val projectRootDir: DirectoryProperty

    @TaskAction
    fun run() {
        println("========================================")
        println("🛡️ 开始【最高安全级别】物理扫描 + 同名残留审计...")

        // 执行期从 Property 中安全地拿到根目录 File 对象
        val rootDirFile = projectRootDir.get().asFile
        val allImages = mutableListOf<File>()

        // 1. 使用标准的 java.io.File 配合 walk() 代替 project.fileTree，彻底对齐配置缓存
        rootDirFile.walk()
            .onEnter { dir ->
                // 排除 build 文件夹和 TUIKit，极大提升扫描性能
                dir.name != "build" && dir.name != "TUIKit"
            }
            .forEach { file ->
                if (file.isFile) {
                    val path = file.absolutePath.replace("\\", "/")
                    // 匹配 drawable / mipmap 或者是 uiFolder 下的资源
                    val isResource = (path.contains("/src/main/res/drawable") ||
                            path.contains("/src/main/res/mipmap") ||
                            path.contains("/src/uiFolder/"))

                    if (isResource && (file.extension in listOf("png", "webp", "jpg", "xml"))) {
                        allImages.add(file)
                    }
                }
            }

        // 2. 收集所有文本内容
        val allTextContent = StringBuilder()
        rootDirFile.walk()
            .onEnter { dir -> dir.name != "build" && dir.name != "TUIKit" }
            .forEach { file ->
                if (file.isFile) {
                    val path = file.absolutePath.replace("\\", "/")
                    val isCodeOrXml =
                        (path.contains("/src/main/") || path.contains("/src/uiFolder/")) ||
                                file.name.endsWith(".gradle") || file.name.endsWith(".gradle.kts")

                    if (isCodeOrXml && (file.extension in listOf(
                            "xml",
                            "kt",
                            "java",
                            "gradle",
                            "kts"
                        ))
                    ) {
                        try {
                            allTextContent.append(file.readText())
                        } catch (e: Exception) {
                            // 预防极个别特殊编码文件读取失败
                        }
                    }
                }
            }

        val fullText = allTextContent.toString()

        // 用于审计的集合
        val deletedFilesLog = mutableListOf<String>()
        val deletedBaseNames = mutableSetOf<String>()
        val keptFiles = mutableListOf<File>()

        // 3. 执行鉴定与删除
        allImages.forEach { imgFile ->
            val resName = imgFile.name.replace(Regex("\\.9\\.png$"), "")
                .replace(Regex("\\.[a-zA-Z0-9]+$"), "")
            val pattern = Regex("\\b$resName\\b")
            val isUsed = pattern.containsMatchIn(fullText)

            if (!isUsed) {
                println("🗑️ [安全粉碎] -> ${imgFile.absolutePath}")
                deletedFilesLog.add(imgFile.absolutePath)
                deletedBaseNames.add(resName)
                imgFile.delete() // 物理删除
            } else {
                keptFiles.add(imgFile)
            }
        }

        // 4. 执行同名残留检查 (Cross-check)
        val duplicateResiduals = mutableSetOf<String>()
        keptFiles.forEach { keptFile ->
            val resName = keptFile.name.replace(Regex("\\.9\\.png$"), "")
                .replace(Regex("\\.[a-zA-Z0-9]+$"), "")
            if (deletedBaseNames.contains(resName)) {
                duplicateResiduals.add(keptFile.absolutePath)
            }
        }

        // 5. 生成综合审计报告
        val reportFile = File(rootDirFile, "delete_audit_report.txt")
        var reportContent = "=== 🗑️ 成功删除的文件 (共 ${deletedFilesLog.size} 个) ===\n"
        reportContent += deletedFilesLog.joinToString("\n")

        if (duplicateResiduals.isNotEmpty()) {
            reportContent += "\n\n\n=== ⚠️ 警告：发现同名残留文件 (共 ${duplicateResiduals.size} 个) ===\n"
            reportContent += "说明：以下文件虽然和上述被删文件同名，但因为某些原因（如被其他模块引用，或处于不同分辨率文件夹）被脚本保护下来了，请人工检查是否需要手动删除：\n"
            reportContent += duplicateResiduals.joinToString("\n")
            println("\n⚠️ 警告：发现了 ${duplicateResiduals.size} 个同名残留文件！请查看审计报告。")
        } else {
            reportContent += "\n\n\n=== ✅ 完美：没有发现同名残留文件。 ==="
            println("\n✅ 完美：删除的图片十分干净，没有同名残留。")
        }

        reportFile.writeText(reportContent)

        println("🎉 清理与审计完成！")
        println("📄 详细报告已生成至项目根目录: delete_audit_report.txt")
        println("========================================")
    }
}

// 移除未使用的文件
tasks.register<SafeDeleteImagesTask>("physicalDeleteUnusedImagesSafely") {
    // 允许在配置阶段安全读取 project.layout.projectDirectory
    projectRootDir.set(project.layout.projectDirectory)
}