package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FamilyMember
import com.example.data.model.MedicalBill
import com.example.data.model.MedicalRecordType
import com.example.data.model.MedicalReport
import com.example.data.model.MedicalTimelineItem
import com.example.data.model.Prescription
import com.example.data.util.AttachmentUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PatientSelectorBar
import com.example.ui.components.SearchAndFilterBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    timelineItems: List<MedicalTimelineItem>,
    members: List<FamilyMember>,
    selectedMemberId: Long?,
    searchQuery: String,
    selectedTypeFilter: MedicalRecordType?,
    onlyAttachmentsFilter: Boolean,
    onSelectMember: (Long?) -> Unit,
    onAddMemberClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (MedicalRecordType?) -> Unit,
    onToggleOnlyAttachments: (Boolean) -> Unit,
    onViewBill: (MedicalBill) -> Unit,
    onViewReport: (MedicalReport) -> Unit,
    onViewPrescription: (Prescription) -> Unit,
    onOpenAttachmentViewer: (uri: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Group items date-wise
    val groupedByDate = timelineItems.groupBy { it.formattedDate }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Patient Selector Chip Bar
        item {
            PatientSelectorBar(
                members = members,
                selectedMemberId = selectedMemberId,
                onSelectMember = onSelectMember,
                onAddMemberClick = onAddMemberClick
            )
        }

        // Header and Search Bar
        item {
            SearchAndFilterBar(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Search date timeline & attached docs..."
            )
        }

        // Quick Filter Chips Row
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Types
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { onTypeFilterChange(null) },
                        label = { Text("All Records (${timelineItems.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealContainer,
                            selectedLabelColor = TealPrimary
                        )
                    )
                }

                // Only Attachments toggle chip
                item {
                    FilterChip(
                        selected = onlyAttachmentsFilter,
                        onClick = { onToggleOnlyAttachments(!onlyAttachmentsFilter) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("With Scanned Attachments") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = EmeraldTertiary
                        )
                    )
                }

                // Bills
                item {
                    FilterChip(
                        selected = selectedTypeFilter == MedicalRecordType.BILL,
                        onClick = {
                            onTypeFilterChange(
                                if (selectedTypeFilter == MedicalRecordType.BILL) null else MedicalRecordType.BILL
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Bills") }
                    )
                }

                // Reports
                item {
                    FilterChip(
                        selected = selectedTypeFilter == MedicalRecordType.REPORT,
                        onClick = {
                            onTypeFilterChange(
                                if (selectedTypeFilter == MedicalRecordType.REPORT) null else MedicalRecordType.REPORT
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Reports") }
                    )
                }

                // Prescriptions
                item {
                    FilterChip(
                        selected = selectedTypeFilter == MedicalRecordType.PRESCRIPTION,
                        onClick = {
                            onTypeFilterChange(
                                if (selectedTypeFilter == MedicalRecordType.PRESCRIPTION) null else MedicalRecordType.PRESCRIPTION
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Prescriptions") }
                    )
                }
            }
        }

        // Empty state
        if (timelineItems.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (onlyAttachmentsFilter) "No Scanned Attachments Found" else "No Timeline Records Found",
                    description = if (onlyAttachmentsFilter)
                        "You haven't attached any scanned bills, lab reports, or prescriptions yet. Attach documents to auto-infer insights!"
                    else
                        "Log medical bills, diagnostic lab reports, or prescriptions to build your date-wise health timeline.",
                    icon = Icons.Default.AttachFile
                )
            }
        } else {
            // Render Chronological Groups
            for ((dateHeader, itemsOnDate) in groupedByDate) {
                item(key = "header_$dateHeader") {
                    DateTimelineHeader(dateText = dateHeader, count = itemsOnDate.size)
                }

                items(itemsOnDate, key = { "${it.type.name}_${it.id}" }) { item ->
                    TimelineEventCard(
                        item = item,
                        onClick = {
                            when (item.type) {
                                MedicalRecordType.BILL -> item.billRef?.let { onViewBill(it) }
                                MedicalRecordType.REPORT -> item.reportRef?.let { onViewReport(it) }
                                MedicalRecordType.PRESCRIPTION -> item.rxRef?.let { onViewPrescription(it) }
                            }
                        },
                        onOpenAttachment = {
                            if (!item.attachmentUri.isNullOrBlank()) {
                                onOpenAttachmentViewer(item.attachmentUri, item.title)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DateTimelineHeader(
    dateText: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        Text(
            text = "$count ${if (count == 1) "record" else "records"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TimelineEventCard(
    item: MedicalTimelineItem,
    onClick: () -> Unit,
    onOpenAttachment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (typeColor, typeBg, typeIcon, typeLabel) = when (item.type) {
        MedicalRecordType.BILL -> Quad(TealPrimary, TealContainer, Icons.Default.Receipt, "Medical Bill")
        MedicalRecordType.REPORT -> Quad(CyanSecondary, Color(0xFFE0F7FA), Icons.Default.Science, "Lab Report")
        MedicalRecordType.PRESCRIPTION -> Quad(EmeraldTertiary, EmeraldContainer, Icons.Default.Medication, "Prescription")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("timeline_item_${item.type.name}_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Type badge, Patient name, and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = typeLabel,
                            tint = typeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = typeColor
                            )
                        )
                        Text(
                            text = item.memberName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(statusText = item.statusText)
            }

            // Main Title & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (item.amountOrMetric.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = item.amountOrMetric,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Attached Scanned Document Section (If attached)
            if (!item.attachmentUri.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAttachment() }
                        .testTag("timeline_attachment_preview")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Thumbnail
                        val imageFile = AttachmentUtils.resolveFile(context, item.attachmentUri)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageFile != null && imageFile.exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageFile)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Scanned Attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = EmeraldTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = item.attachmentName ?: "Scanned Document Attached",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Tap to view, download or share",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = EmeraldTertiary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = {
                                    AttachmentUtils.downloadAttachmentToDevice(
                                        context,
                                        item.attachmentUri,
                                        item.attachmentName ?: item.title
                                    )
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download attachment",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onOpenAttachment() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "View",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Insights Highlight
            val insights = item.aiInsights
            if (!insights.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TealContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Insights",
                            tint = TealPrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = "Gemini AI Inferred Summary",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                            )
                            Text(
                                text = insights,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
