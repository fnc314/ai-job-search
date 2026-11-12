@file:OptIn(ExperimentalMaterial3Api::class)

package dev.fnc314.tools.jobtracker.features.companydetails.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.fontawesome.FontAwesome
import com.composables.icons.fontawesome.brands.Linkedin
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.CompanyLogoImage
import dev.fnc314.tools.jobtracker.core.widgets.CompanyLogoSize
import dev.fnc314.tools.jobtracker.core.widgets.CompanyMetricsTextRow
import dev.fnc314.tools.jobtracker.core.widgets.ErrorBannerWidget
import dev.fnc314.tools.jobtracker.core.widgets.FitBadge
import dev.fnc314.tools.jobtracker.core.widgets.JobApplicationCard
import dev.fnc314.tools.jobtracker.core.widgets.LoadingWidget
import dev.fnc314.tools.jobtracker.core.widgets.TooltipIconButton
import dev.fnc314.tools.jobtracker.core.widgets.TopLevelCardSurfaceVariant
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_applications_header
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_avg_fit_label
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_industry_label
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_locations_label
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_no_applications
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_open_linkedin_tooltip
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_open_website_tooltip
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_salary_label
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_source_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen presenting detailed profile metrics and associated job applications for a single company.
 *
 * @param companyId Canonical identifier of the company to load.
 * @param onNavigateBack Callback invoked when navigating back.
 * @param onNavigateToApplication Callback invoked when a job application card is selected.
 * @param modifier Layout modifier.
 * @param onOpenUrl Optional callback for opening external company URLs (website, LinkedIn).
 * @param viewModel [CompanyDetailsViewModel] managing UI state.
 */
@Composable
public fun CompanyDetailsScreen(
  companyId: String,
  onNavigateBack: () -> Unit,
  onNavigateToApplication: (applicationId: String) -> Unit,
  modifier: Modifier = Modifier,
  onOpenUrl: ((String) -> Unit)? = null,
  viewModel: CompanyDetailsViewModel = koinViewModel(),
) {
  LaunchedEffect(companyId) {
    viewModel.loadCompany(companyId)
  }

  val state by viewModel.uiState.collectAsState()

  when (val s = state) {
    is CompanyDetailsUiState.Loading -> {
      LoadingWidget(modifier = modifier.fillMaxSize())
    }

    is CompanyDetailsUiState.Error -> {
      ErrorBannerWidget(
        message = s.message,
        onRetry = { viewModel.loadCompany(companyId, forceRefresh = true) },
        modifier = modifier.fillMaxSize(),
      )
    }

    is CompanyDetailsUiState.Content -> {
      CompanyDetailsContent(
        details = s.details,
        isRefreshing = s.isRefreshing,
        onRefresh = { viewModel.loadCompany(companyId, forceRefresh = true) },
        onNavigateBack = onNavigateBack,
        onNavigateToApplication = onNavigateToApplication,
        onOpenUrl = onOpenUrl,
        modifier = modifier,
      )
    }
  }
}

@Composable
internal fun CompanyDetailsContent(
  details: CompanyDetailsUiModel,
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  onNavigateBack: () -> Unit,
  onNavigateToApplication: (applicationId: String) -> Unit,
  onOpenUrl: ((String) -> Unit)?,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      modifier = Modifier.fillMaxSize(),
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
      ) {
        // Overview Card
        item {
          CompanyOverviewCard(
            details = details,
            onOpenUrl = onOpenUrl,
          )
        }

        // Section header: Applications
        item {
          Text(
            text = stringResource(
              Res.string.company_details_applications_header,
              details.applications.size,
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = MaterialTheme.spacing.small),
          )
        }

        if (details.applications.isEmpty()) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              shape = MaterialTheme.shapes.medium,
            ) {
              Text(
                text = stringResource(Res.string.company_details_no_applications),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
              )
            }
          }
        } else {
          items(details.applications, key = { it.id }) { appModel ->
            JobApplicationCard(
              model = appModel,
              onApplicationClick = { onNavigateToApplication(it) },
              variant = TopLevelCardSurfaceVariant.Highlighted,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CompanyOverviewCard(
  details: CompanyDetailsUiModel,
  onOpenUrl: ((String) -> Unit)?,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    shape = MaterialTheme.shapes.medium,
    elevation = CardDefaults.elevatedCardElevation(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
      // Header: Logo, Name, Industry, Website, Globe Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
          modifier = Modifier.weight(1f),
        ) {
          CompanyLogoImage(
            companyName = details.companyName,
            logoUrl = details.logoUrl,
            companyInitial = details.companyInitial,
            logoSize = CompanyLogoSize.Large,
          )

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = details.companyName,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            if (!details.industry.isNullOrBlank()) {
              Text(
                text = "${stringResource(Res.string.company_details_industry_label)} ${details.industry}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (!details.website.isNullOrBlank()) {
              Text(
                text = details.website,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        if (onOpenUrl != null) {
          Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
            if (!details.website.isNullOrBlank()) {
              TooltipIconButton(
                tooltip = stringResource(
                  Res.string.company_details_open_website_tooltip,
                  details.companyName,
                ),
                icon = Icons.Default.Public,
                onClick = { onOpenUrl(details.website) },
              )
            }
            if (!details.linkedinUrl.isNullOrBlank()) {
              TooltipIconButton(
                tooltip = stringResource(
                  Res.string.company_details_open_linkedin_tooltip,
                  details.companyName,
                ),
                icon = FontAwesome.Brands.Linkedin,
                onClick = { onOpenUrl(details.linkedinUrl) },
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

      // Semantic status counts & percentages row
      CompanyMetricsTextRow(
        draftCount = details.draftCount,
        draftPercentage = details.draftPercentage,
        submittedCount = details.submittedCount,
        submittedPercentage = details.submittedPercentage,
        interviewingCount = details.interviewingCount,
        interviewingPercentage = details.interviewingPercentage,
        offerCount = details.offerCount,
        offerPercentage = details.offerPercentage,
        rejectedCount = details.rejectedCount,
        rejectedPercentage = details.rejectedPercentage,
      )

      Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

      // Metadata summary
      if (details.averageFitScore > 0.0) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
          Text(
            text = stringResource(Res.string.company_details_avg_fit_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FitBadge(score = details.averageFitScore.toInt())
        }
      }

      if (details.salaryRangeDisplay.isNotBlank()) {
        Text(
          text = "${stringResource(Res.string.company_details_salary_label)} ${details.salaryRangeDisplay}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (details.primaryChannelDisplay.isNotBlank()) {
        Text(
          text = "${stringResource(Res.string.company_details_source_label)} ${details.primaryChannelDisplay}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (details.locationsText.isNotBlank()) {
        Text(
          text = "${stringResource(Res.string.company_details_locations_label)} ${details.locationsText}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
