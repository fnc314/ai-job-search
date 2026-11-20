package dev.fnc314.aijobtracker.frontend.features.settings.domain.usecase

/**
 * Functional interface usecase invoking device deregistration from push notifications.
 */
public fun interface DeregisterDeviceNotificationsUseCase {
    /**
     * Deregisters the current host device from receiving push notifications.
     *
     * @return True if deregistration succeeded on backend or was handled cleanly.
     */
    public suspend operator fun invoke(): Boolean
}
