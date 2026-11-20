package dev.fnc314.aijobtracker.records.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostingSimilarityTest {
  private val stripe =
    PostingSimilarity.Candidate(
      id = "stripe--android-engineer-identity",
      companyId = "stripe",
      role = "Android Engineer, Identity",
      jobUrl = "https://stripe.com/careers/listing/android-engineer-identity/8311069",
      status = "SUBMITTED",
    )

  @Test
  fun sameUrlIgnoringQueryIsMatch() {
    val matches =
      PostingSimilarity.findSimilar(
        "Stripe",
        "Something Else",
        "https://stripe.com/careers/listing/android-engineer-identity/8311069?gh_src=73vnei",
        listOf(stripe),
      )
    assertEquals("same posting URL", matches.single().reason)
  }

  @Test
  fun sameDocumentIdIsMatch() {
    val matches = PostingSimilarity.findSimilar("Stripe", "Android Engineer, Identity", null, listOf(stripe))
    assertEquals(1.0, matches.single().score)
  }

  @Test
  fun similarRoleSameCompanyIsMatch() {
    val matches = PostingSimilarity.findSimilar("Stripe, Inc.", "Senior Android Engineer - Identity", null, listOf(stripe))
    assertTrue(matches.isNotEmpty())
  }

  @Test
  fun differentRoleOrCompanyIsNotMatch() {
    assertTrue(PostingSimilarity.findSimilar("Stripe", "Backend Engineer, Payments", null, listOf(stripe)).isEmpty())
    assertTrue(PostingSimilarity.findSimilar("Moonshot", "Android Engineer, Identity", null, listOf(stripe)).isEmpty())
  }

  @Test
  fun unknownStatusFallsBackToNeutral() {
    assertEquals(StatusNormalizer.UNSPECIFIED_STATUS, StatusNormalizer.normalizeStatus("garbage"))
    assertEquals(StatusNormalizer.UNSPECIFIED_STATUS, StatusNormalizer.normalizeStatus("scraped"))
    assertEquals(JobApplicationStatus.Active.Scraped, JobApplicationStatus.parse("garbage"))
  }
}
