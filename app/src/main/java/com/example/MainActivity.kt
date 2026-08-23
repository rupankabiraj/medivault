package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.dialogs.AddEditBillSheet
import com.example.ui.dialogs.AddEditMemberDialog
import com.example.ui.dialogs.AddEditPrescriptionSheet
import com.example.ui.dialogs.AddEditReportSheet
import com.example.ui.dialogs.BillDetailDialog
import com.example.ui.dialogs.HealthSummaryExportDialog
import com.example.ui.dialogs.PrescriptionDetailDialog
import com.example.ui.dialogs.RefillMedicationDialog
import com.example.ui.dialogs.ReportDetailDialog
import com.example.ui.screens.AnalyticsAndFamilyScreen
import com.example.ui.screens.BillsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PrescriptionsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MediVaultApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediVaultApp(
    viewModel: MainViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedMemberId by viewModel.selectedMemberId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val billCategoryFilter by viewModel.billCategoryFilter.collectAsStateWithLifecycle()
    val billStatusFilter by viewModel.billStatusFilter.collectAsStateWithLifecycle()
    val reportCategoryFilter by viewModel.reportCategoryFilter.collectAsStateWithLifecycle()
    val reportStatusFilter by viewModel.reportStatusFilter.collectAsStateWithLifecycle()

    val members by viewModel.allMembers.collectAsStateWithLifecycle()
    val allBills by viewModel.allBills.collectAsStateWithLifecycle()
    val filteredBills by viewModel.filteredBills.collectAsStateWithLifecycle()
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()
    val filteredReports by viewModel.filteredReports.collectAsStateWithLifecycle()
    val allPrescriptions by viewModel.allPrescriptions.collectAsStateWithLifecycle()
    val filteredPrescriptions by viewModel.filteredPrescriptions.collectAsStateWithLifecycle()
    val allMedications by viewModel.allMedications.collectAsStateWithLifecycle()
    val scheduledDoses by viewModel.todayScheduledDoses.collectAsStateWithLifecycle()
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()

    // Dialog & Sheet States
    val showAddBillSheet by viewModel.showAddBillSheet.collectAsStateWithLifecycle()
    val editingBill by viewModel.editingBill.collectAsStateWithLifecycle()
    val viewingBill by viewModel.viewingBill.collectAsStateWithLifecycle()

    val showAddReportSheet by viewModel.showAddReportSheet.collectAsStateWithLifecycle()
    val editingReport by viewModel.editingReport.collectAsStateWithLifecycle()
    val viewingReport by viewModel.viewingReport.collectAsStateWithLifecycle()

    val showAddPrescriptionSheet by viewModel.showAddPrescriptionSheet.collectAsStateWithLifecycle()
    val editingPrescription by viewModel.editingPrescription.collectAsStateWithLifecycle()
    val viewingPrescription by viewModel.viewingPrescription.collectAsStateWithLifecycle()

    val showAddMemberDialog by viewModel.showAddMemberDialog.collectAsStateWithLifecycle()
    val editingMember by viewModel.editingMember.collectAsStateWithLifecycle()

    val refillMedication by viewModel.showRefillDialog.collectAsStateWithLifecycle()
    val showExportSummary by viewModel.showExportSummary.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MediVault",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Bills • Reports • Prescriptions",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showExportSummary.value = true },
                        modifier = Modifier.testTag("topbar_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "Export Dossier",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                // Tab 0: Home / Dashboard
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = TealContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // Tab 1: Bills
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (dashboardSummary.pendingBillsCount > 0) {
                                    Badge(containerColor = StatusDanger) {
                                        Text("${dashboardSummary.pendingBillsCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Default.ReceiptLong else Icons.Outlined.Receipt,
                                contentDescription = "Bills"
                            )
                        }
                    },
                    label = { Text("Bills", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = TealContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_bills")
                )

                // Tab 2: Reports
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (dashboardSummary.attentionReportsCount > 0) {
                                    Badge(containerColor = StatusDanger) {
                                        Text("${dashboardSummary.attentionReportsCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Default.Science else Icons.Outlined.Science,
                                contentDescription = "Reports"
                            )
                        }
                    },
                    label = { Text("Reports", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = TealContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_reports")
                )

                // Tab 3: Prescriptions & Meds
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (dashboardSummary.lowStockMedsCount > 0) {
                                    Badge(containerColor = StatusDanger) {
                                        Text("${dashboardSummary.lowStockMedsCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Default.Medication else Icons.Outlined.Medication,
                                contentDescription = "Prescriptions"
                            )
                        }
                    },
                    label = { Text("Rx & Meds", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = TealContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_prescriptions")
                )

                // Tab 4: Analytics & Family
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setTab(4) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 4) Icons.Default.BarChart else Icons.Outlined.Analytics,
                            contentDescription = "Analytics"
                        )
                    },
                    label = { Text("Insights", fontSize = 11.sp, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = TealContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_insights")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "main_screen_transition"
            ) { tab ->
                when (tab) {
                    0 -> DashboardScreen(
                        summary = dashboardSummary,
                        members = members,
                        selectedMemberId = selectedMemberId,
                        scheduledDoses = scheduledDoses,
                        recentBills = allBills,
                        recentReports = allReports,
                        recentPrescriptions = allPrescriptions,
                        onSelectMember = { viewModel.selectMember(it) },
                        onAddMemberClick = { viewModel.showAddMemberDialog.value = true },
                        onToggleDose = { viewModel.toggleDose(it) },
                        onNavigateToTab = { viewModel.setTab(it) },
                        onAddBillClick = {
                            viewModel.editingBill.value = null
                            viewModel.showAddBillSheet.value = true
                        },
                        onAddReportClick = {
                            viewModel.editingReport.value = null
                            viewModel.showAddReportSheet.value = true
                        },
                        onAddPrescriptionClick = {
                            viewModel.editingPrescription.value = null
                            viewModel.showAddPrescriptionSheet.value = true
                        },
                        onViewBill = { viewModel.viewingBill.value = it },
                        onViewReport = { viewModel.viewingReport.value = it },
                        onViewPrescription = { viewModel.viewingPrescription.value = it },
                        onExportSummaryClick = { viewModel.showExportSummary.value = true }
                    )

                    1 -> BillsScreen(
                        bills = filteredBills,
                        members = members,
                        selectedMemberId = selectedMemberId,
                        searchQuery = searchQuery,
                        categoryFilter = billCategoryFilter,
                        statusFilter = billStatusFilter,
                        onSelectMember = { viewModel.selectMember(it) },
                        onAddMemberClick = { viewModel.showAddMemberDialog.value = true },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategoryFilterChange = { viewModel.setBillCategoryFilter(it) },
                        onStatusFilterChange = { viewModel.setBillStatusFilter(it) },
                        onAddBillClick = {
                            viewModel.editingBill.value = null
                            viewModel.showAddBillSheet.value = true
                        },
                        onViewBill = { viewModel.viewingBill.value = it },
                        onEditBill = {
                            viewModel.editingBill.value = it
                            viewModel.showAddBillSheet.value = true
                        },
                        onDeleteBill = { viewModel.deleteBill(it) },
                        onTogglePaid = { viewModel.toggleBillPaid(it) }
                    )

                    2 -> ReportsScreen(
                        reports = filteredReports,
                        members = members,
                        selectedMemberId = selectedMemberId,
                        searchQuery = searchQuery,
                        categoryFilter = reportCategoryFilter,
                        statusFilter = reportStatusFilter,
                        onSelectMember = { viewModel.selectMember(it) },
                        onAddMemberClick = { viewModel.showAddMemberDialog.value = true },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategoryFilterChange = { viewModel.setReportCategoryFilter(it) },
                        onStatusFilterChange = { viewModel.setReportStatusFilter(it) },
                        onAddReportClick = {
                            viewModel.editingReport.value = null
                            viewModel.showAddReportSheet.value = true
                        },
                        onViewReport = { viewModel.viewingReport.value = it },
                        onEditReport = {
                            viewModel.editingReport.value = it
                            viewModel.showAddReportSheet.value = true
                        },
                        onDeleteReport = { viewModel.deleteReport(it) },
                        parseBiomarkers = { viewModel.parseBiomarkers(it) }
                    )

                    3 -> PrescriptionsScreen(
                        prescriptions = filteredPrescriptions,
                        medications = allMedications,
                        members = members,
                        selectedMemberId = selectedMemberId,
                        searchQuery = searchQuery,
                        onSelectMember = { viewModel.selectMember(it) },
                        onAddMemberClick = { viewModel.showAddMemberDialog.value = true },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onAddPrescriptionClick = {
                            viewModel.editingPrescription.value = null
                            viewModel.showAddPrescriptionSheet.value = true
                        },
                        onViewPrescription = { viewModel.viewingPrescription.value = it },
                        onEditPrescription = {
                            viewModel.editingPrescription.value = it
                            viewModel.showAddPrescriptionSheet.value = true
                        },
                        onDeletePrescription = { viewModel.deletePrescription(it) },
                        onRefillClick = { viewModel.showRefillDialog.value = it }
                    )

                    4 -> AnalyticsAndFamilyScreen(
                        summary = dashboardSummary,
                        members = members,
                        selectedMemberId = selectedMemberId,
                        onSelectMember = { viewModel.selectMember(it) },
                        onAddMemberClick = {
                            viewModel.editingMember.value = null
                            viewModel.showAddMemberDialog.value = true
                        },
                        onEditMember = {
                            viewModel.editingMember.value = it
                            viewModel.showAddMemberDialog.value = true
                        },
                        onDeleteMember = { viewModel.deleteMember(it) },
                        onExportSummaryClick = { viewModel.showExportSummary.value = true }
                    )
                }
            }
        }
    }

    // Modal Sheets & Dialogs
    if (showAddBillSheet || editingBill != null) {
        AddEditBillSheet(
            bill = editingBill,
            members = members,
            selectedMemberId = selectedMemberId,
            onDismiss = {
                viewModel.showAddBillSheet.value = false
                viewModel.editingBill.value = null
            },
            onSave = { id, memberId, provider, doctor, date, category, invoiceNum, total, insured, outOfPocket, status, dueDate, notes, lineItems ->
                viewModel.saveBill(
                    id, memberId, provider, doctor, date, category, invoiceNum, total, insured, outOfPocket, status, dueDate, notes, lineItems
                )
            }
        )
    }

    if (showAddReportSheet || editingReport != null) {
        AddEditReportSheet(
            report = editingReport,
            members = members,
            selectedMemberId = selectedMemberId,
            onDismiss = {
                viewModel.showAddReportSheet.value = false
                viewModel.editingReport.value = null
            },
            onSave = { id, memberId, testName, category, reportDate, facility, doctor, summary, status, biomarkers, followUpDate, notes ->
                viewModel.saveReport(
                    id, memberId, testName, category, reportDate, facility, doctor, summary, status, biomarkers, followUpDate, notes
                )
            }
        )
    }

    if (showAddPrescriptionSheet || editingPrescription != null) {
        val currentRxMeds = if (editingPrescription != null) {
            allMedications.filter { it.prescriptionId == editingPrescription!!.id }
        } else {
            emptyList()
        }

        AddEditPrescriptionSheet(
            prescription = editingPrescription,
            medications = currentRxMeds,
            members = members,
            selectedMemberId = selectedMemberId,
            onDismiss = {
                viewModel.showAddPrescriptionSheet.value = false
                viewModel.editingPrescription.value = null
            },
            onSave = { rxId, memberId, doctor, specialty, clinic, diagnosis, datePrescribed, durationDays, isOngoing, followUpDate, advice, status, medicationsList ->
                viewModel.savePrescriptionWithMedications(
                    rxId, memberId, doctor, specialty, clinic, diagnosis, datePrescribed, durationDays, isOngoing, followUpDate, advice, status, medicationsList
                )
            }
        )
    }

    if (showAddMemberDialog || editingMember != null) {
        AddEditMemberDialog(
            member = editingMember,
            onDismiss = {
                viewModel.showAddMemberDialog.value = false
                viewModel.editingMember.value = null
            },
            onSave = { id, name, relationship, age, bloodGroup, allergies, emergencyContact, colorHex ->
                viewModel.saveMember(
                    id, name, relationship, age, bloodGroup, allergies, emergencyContact, colorHex
                )
            }
        )
    }

    refillMedication?.let { med ->
        RefillMedicationDialog(
            medication = med,
            onDismiss = { viewModel.showRefillDialog.value = null },
            onRefill = { count -> viewModel.refillMedication(med.id, count) }
        )
    }

    viewingBill?.let { bill ->
        val member = members.find { it.id == bill.memberId }
        BillDetailDialog(
            bill = bill,
            member = member,
            onDismiss = { viewModel.viewingBill.value = null },
            onEdit = {
                viewModel.viewingBill.value = null
                viewModel.editingBill.value = bill
                viewModel.showAddBillSheet.value = true
            },
            onDelete = {
                viewModel.deleteBill(bill)
            }
        )
    }

    viewingReport?.let { report ->
        val member = members.find { it.id == report.memberId }
        val biomarkers = viewModel.parseBiomarkers(report.biomarkersRaw)
        ReportDetailDialog(
            report = report,
            member = member,
            biomarkers = biomarkers,
            onDismiss = { viewModel.viewingReport.value = null },
            onEdit = {
                viewModel.viewingReport.value = null
                viewModel.editingReport.value = report
                viewModel.showAddReportSheet.value = true
            },
            onDelete = {
                viewModel.deleteReport(report)
            }
        )
    }

    viewingPrescription?.let { rx ->
        val member = members.find { it.id == rx.memberId }
        val rxMeds = allMedications.filter { it.prescriptionId == rx.id }
        PrescriptionDetailDialog(
            prescription = rx,
            medications = rxMeds,
            member = member,
            onDismiss = { viewModel.viewingPrescription.value = null },
            onEdit = {
                viewModel.viewingPrescription.value = null
                viewModel.editingPrescription.value = rx
                viewModel.showAddPrescriptionSheet.value = true
            },
            onDelete = {
                viewModel.deletePrescription(rx)
            }
        )
    }

    if (showExportSummary) {
        HealthSummaryExportDialog(
            members = members,
            bills = allBills,
            reports = allReports,
            prescriptions = allPrescriptions,
            medications = allMedications,
            selectedMemberId = selectedMemberId,
            onDismiss = { viewModel.showExportSummary.value = false }
        )
    }
}
