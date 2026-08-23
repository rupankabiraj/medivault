package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Biomarker
import com.example.data.model.FamilyMember
import com.example.data.model.MedicalReport
import com.example.data.model.ReportCategory
import com.example.data.model.ReportStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PatientSelectorBar
import com.example.ui.components.SearchAndFilterBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    reports: List<MedicalReport>,
    members: List<FamilyMember>,
    selectedMemberId: Long?,
    searchQuery: String,
    categoryFilter: String?,
    statusFilter: String?,
    onSelectMember: (Long?) -> Unit,
    onAddMemberClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onAddReportClick: () -> Unit,
    onViewReport: (MedicalReport) -> Unit,
    onEditReport: (MedicalReport) -> Unit,
    onDeleteReport: (MedicalReport) -> Unit,
    parseBiomarkers: (String) -> List<Biomarker>,
    modifier: Modifier = Modifier
) {
    val memberMap = members.associateBy { it.id }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val attentionCount = reports.count { it.status == ReportStatus.ATTENTION_REQUIRED.name || it.status == ReportStatus.CRITICAL.name }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Patient selector
            item {
                PatientSelectorBar(
                    members = members,
                    selectedMemberId = selectedMemberId,
                    onSelectMember = onSelectMember,
                    onAddMemberClick = onAddMemberClick
                )
            }

            // Search Bar
            item {
                SearchAndFilterBar(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    placeholder = "Search lab test, biomarker, facility, doctor..."
                )
            }

            // Category & Status Filter Chips
            item {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = categoryFilter == null && statusFilter == null,
                        onClick = {
                            onCategoryFilterChange(null)
                            onStatusFilterChange(null)
                        },
                        label = { Text("All Reports (${reports.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    for (status in ReportStatus.values()) {
                        FilterChip(
                            selected = statusFilter == status.name,
                            onClick = { onStatusFilterChange(status.name) },
                            label = { Text(status.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }

                    for (cat in ReportCategory.values()) {
                        FilterChip(
                            selected = categoryFilter == cat.name,
                            onClick = { onCategoryFilterChange(cat.name) },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // High Alert Banner (if any)
            if (attentionCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusWarningContainer.copy(alpha = 0.8f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Attention",
                                tint = StatusWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "$attentionCount Report(s) with Elevated / Out-of-Range Markers",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Consult your physician regarding flagged lab test parameters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }
                }
            }

            // Reports List
            if (reports.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No diagnostic reports found",
                        description = if (searchQuery.isNotBlank() || categoryFilter != null) "No lab reports match your current filters." else "Upload diagnostic lab tests, blood work, radiology scans, and pathology findings.",
                        icon = Icons.Outlined.Science,
                        actionButtonText = "+ Add Diagnostic Report",
                        onActionClick = onAddReportClick
                    )
                }
            } else {
                items(reports, key = { it.id }) { report ->
                    val member = memberMap[report.memberId]
                    val biomarkers = parseBiomarkers(report.biomarkersRaw)
                    ReportListItemCard(
                        report = report,
                        member = member,
                        biomarkers = biomarkers,
                        dateFormat = dateFormat,
                        onView = { onViewReport(report) },
                        onEdit = { onEditReport(report) },
                        onDelete = { onDeleteReport(report) }
                    )
                }
            }
        }

        // Add Report FAB
        FloatingActionButton(
            onClick = onAddReportClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
                .testTag("fab_add_report"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Report")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Report", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReportListItemCard(
    report: MedicalReport,
    member: FamilyMember?,
    biomarkers: List<Biomarker>,
    dateFormat: SimpleDateFormat,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val abnormalMarkers = biomarkers.filter { it.isAbnormal }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onView() }
            .testTag("report_card_${report.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Test Name, Lab Facility & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = report.testName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${report.labOrFacility} ${if (report.orderingDoctor.isNotBlank()) "• Dr. ${report.orderingDoctor}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                StatusBadge(statusText = report.status)
            }

            // Summary Findings preview
            if (report.summaryFindings.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Key Clinical Findings:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = report.summaryFindings,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Biomarkers Chips (Highlighting abnormal ones)
            if (biomarkers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayMarkers = biomarkers.take(3)
                    for (bm in displayMarkers) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (bm.isAbnormal) StatusDangerContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${bm.name}:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (bm.isAbnormal) StatusDanger else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${bm.value} ${bm.unit}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = if (bm.isAbnormal) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    if (biomarkers.size > 3) {
                        Text(
                            text = "+${biomarkers.size - 3} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row: Member Name, Date, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${member?.name ?: "Family"} • ${dateFormat.format(Date(report.reportDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Report",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Report",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
