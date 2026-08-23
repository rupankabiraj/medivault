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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Medication
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
import com.example.data.model.FamilyMember
import com.example.data.model.Medication
import com.example.data.model.Prescription
import com.example.data.model.PrescriptionStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PatientSelectorBar
import com.example.ui.components.SearchAndFilterBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrescriptionsScreen(
    prescriptions: List<Prescription>,
    medications: List<Medication>,
    members: List<FamilyMember>,
    selectedMemberId: Long?,
    searchQuery: String,
    onSelectMember: (Long?) -> Unit,
    onAddMemberClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onAddPrescriptionClick: () -> Unit,
    onViewPrescription: (Prescription) -> Unit,
    onEditPrescription: (Prescription) -> Unit,
    onDeletePrescription: (Prescription) -> Unit,
    onRefillClick: (Medication) -> Unit,
    modifier: Modifier = Modifier
) {
    val memberMap = members.associateBy { it.id }
    val medsByRx = medications.groupBy { it.prescriptionId }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val lowStockMeds = medications.filter { it.isActive && it.pillsRemaining <= it.refillThreshold }

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
                    placeholder = "Search doctor, diagnosis, medication, hospital..."
                )
            }

            // Low Stock Refill Banner
            if (lowStockMeds.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusDangerContainer.copy(alpha = 0.7f)
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
                                contentDescription = "Low Stock Alert",
                                tint = StatusDanger,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${lowStockMeds.size} Medication(s) Running Low on Pills",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = lowStockMeds.joinToString(", ") { "${it.medicineName} (${it.pillsRemaining} left)" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7F1D1D),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Prescriptions List
            if (prescriptions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No prescriptions recorded",
                        description = if (searchQuery.isNotBlank()) "No prescriptions match your search query." else "Keep your doctor prescriptions, daily dosage timings, and pill refill counts organized here.",
                        icon = Icons.Outlined.Medication,
                        actionButtonText = "+ Add New Prescription",
                        onActionClick = onAddPrescriptionClick
                    )
                }
            } else {
                items(prescriptions, key = { it.id }) { rx ->
                    val member = memberMap[rx.memberId]
                    val rxMeds = medsByRx[rx.id] ?: emptyList()

                    PrescriptionCard(
                        prescription = rx,
                        medications = rxMeds,
                        member = member,
                        dateFormat = dateFormat,
                        onView = { onViewPrescription(rx) },
                        onEdit = { onEditPrescription(rx) },
                        onDelete = { onDeletePrescription(rx) },
                        onRefillClick = onRefillClick
                    )
                }
            }
        }

        // Add Rx FAB
        FloatingActionButton(
            onClick = onAddPrescriptionClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
                .testTag("fab_add_prescription"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Prescription")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Prescription", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PrescriptionCard(
    prescription: Prescription,
    medications: List<Medication>,
    member: FamilyMember?,
    dateFormat: SimpleDateFormat,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRefillClick: (Medication) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onView() }
            .testTag("prescription_card_${prescription.id}"),
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
            // Header Row: Doctor Name, Clinic, Diagnosis & Status
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
                            .background(TealContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = prescription.diagnosis,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${prescription.doctorName} (${prescription.specialty})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = prescription.clinicOrHospital,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                StatusBadge(statusText = prescription.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prescribed Medicines list inside this Rx
            Text(
                text = "Prescribed Medicines (${medications.size}):",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            for (med in medications) {
                val isLowStock = med.pillsRemaining <= med.refillThreshold
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLowStock) StatusDangerContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = med.medicineName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "(${med.dosage})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${med.frequency} • ${med.timing} ${if (med.instructions.isNotBlank()) "• ${med.instructions}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Stock Pill Counter & Refill Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isLowStock) StatusDangerContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "${med.pillsRemaining} left",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isLowStock) StatusDanger else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRefillClick(med) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Refill Medication",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (prescription.doctorAdvice.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Advice: ${prescription.doctorAdvice}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Member Name, Date Prescribed, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${member?.name ?: "Family"} • Prescribed ${dateFormat.format(Date(prescription.datePrescribed))}",
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
                            contentDescription = "Edit Prescription",
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
                            contentDescription = "Delete Prescription",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
