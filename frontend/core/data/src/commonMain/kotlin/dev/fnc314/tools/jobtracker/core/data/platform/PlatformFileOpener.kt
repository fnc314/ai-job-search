package dev.fnc314.tools.jobtracker.core.data.platform

/**
 * Multiplatform launcher providing operating-system native document, web link, and PDF viewing.
 *
 * Implemented using Android Intents, Java Desktop AWT, iOS UIKit / SafariServices, and browser window APIs on Web.
 */
public expect class PlatformFileOpener() {

    /**
     * Opens a web link in the system default internet browser.
     *
     * @param url Fully-qualified web URL to navigate to.
     */
    public fun openUrl(url: String): Unit

    /**
     * Opens a remote or local PDF document using the platform-native PDF viewer.
     *
     * @param url Direct URL referencing the PDF artifact.
     */
    public fun openPdf(url: String): Unit

    /**
     * Opens a local filesystem file in its default associated desktop/mobile application.
     *
     * @param path Absolute local filesystem path to the file.
     */
    public fun openFile(path: String): Unit
}
