package dev.fnc314.tools.jobtracker.kmp.gradle.plugins

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.KotlinMultiplatformAndroidPlugin
import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.fromCatalog
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Common internal contract providing typed catalog access and execution hooks. */
internal interface BaseConventionPlugin {
    public fun <T> Project.fromCatalog(item: CatalogTypes<T>): T = item.fromCatalog(this)

    public fun Project.configurePlugin(): Unit
}

/** Typed internal contract for AGP-integrated plugins. */
internal interface ProjectPlugins<
    AndroidPlugin : Plugin<Project>,
    Ext : AndroidComponentsExtension<*, *, *>
> : BaseConventionPlugin {

    public val extensionClass: Class<Ext>
    public val pluginClass: Class<AndroidPlugin>

    public fun Project.androidExtension(): Ext = extensions.getByType(extensionClass)

    public interface AndroidApplication : ProjectPlugins<AppPlugin, ApplicationAndroidComponentsExtension> {
        override val pluginClass: Class<AppPlugin> get() = AppPlugin::class.java
        override val extensionClass: Class<ApplicationAndroidComponentsExtension>
            get() = ApplicationAndroidComponentsExtension::class.java
    }

    public interface AndroidLibrary : ProjectPlugins<LibraryPlugin, LibraryAndroidComponentsExtension> {
        override val pluginClass: Class<LibraryPlugin> get() = LibraryPlugin::class.java
        override val extensionClass: Class<LibraryAndroidComponentsExtension>
            get() = LibraryAndroidComponentsExtension::class.java
    }

    public interface MultiplatformLibrary :
        ProjectPlugins<KotlinMultiplatformAndroidPlugin, KotlinMultiplatformAndroidComponentsExtension> {
        override val pluginClass: Class<KotlinMultiplatformAndroidPlugin>
            get() = KotlinMultiplatformAndroidPlugin::class.java
        override val extensionClass: Class<KotlinMultiplatformAndroidComponentsExtension>
            get() = KotlinMultiplatformAndroidComponentsExtension::class.java
    }
}
