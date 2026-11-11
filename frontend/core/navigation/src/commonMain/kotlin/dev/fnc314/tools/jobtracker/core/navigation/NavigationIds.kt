package dev.fnc314.tools.jobtracker.core.navigation

/**
 * Type-safe wrapper for a job application identifier used in navigation routes.
 *
 * @property value Underlying string ID (typically a Firestore document ID).
 */
@JvmInline
public value class ApplicationId(public val value: String)

/**
 * Type-safe wrapper for a company identifier used in navigation routes.
 *
 * @property value Underlying string ID (typically a company slug).
 */
@JvmInline
public value class CompanyId(public val value: String)
