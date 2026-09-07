@file:OptIn(ExperimentalMaterial3Api::class)
package com.society.buildingmanager

import kotlinx.coroutines.withContext
import android.util.Log
import java.util.Date
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.coroutines.Dispatchers
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import android.util.Base64
import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase FIRST
        com.google.firebase.FirebaseApp.initializeApp(this)

        setContent {
            // Get database INSIDE setContent where Firebase is already initialized
            val database = Firebase.database
            LaunchedEffect(Unit) {
                FirebaseMessaging.getInstance().subscribeToTopic("announcements")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            println("Successfully subscribed to community announcements!")
                        }
                    }
            }
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppNavigation(database = database)
                }
            }
        }
    }
}

data class Complaint(
    val id: String = "",
    val unit: String = "",
    val description: String = "",
    val status: String = "",
    val timestamp: String = "",
    val category: String = ""
)

data class MaintenanceRecord(
    val unit: String = "",
    val isPaid: Boolean = false,
    val amount: Int = 0,
    val dueDate: String = ""
)

data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val subtitle: String,
    val color: Color
)

data class HallBooking(
    val id: String = "",
    val hallName: String = "",
    val date: String = "",
    val startTime: String = "",
    val duration: String = "",
    val amount: String = "",
    val paymentStatus: String = ""
)

data class Booking(
    val hallName: String = "",
    val date: String = "",
    val durationHours: Int = 0
)
// 1. Navigation Logic with State Management
@Composable
fun MainAppNavigation(database: FirebaseDatabase) {
    var currentScreen by remember { mutableStateOf("Dashboard") }
    var isAdmin by remember { mutableStateOf(false) }

    val complaints = remember { mutableStateListOf<Complaint>() }
    val noticesStateList = remember { mutableStateListOf<Triple<String, String, String>>() }
    val bookings = remember { mutableStateListOf<Booking>() }
    val maintenanceRecords = remember {
        mutableStateMapOf<String, MaintenanceRecord>().apply {
            put("A-101", MaintenanceRecord("A-101", false, 1200, "10 May 2026"))
            put("A-102", MaintenanceRecord("A-102", true, 1200, "10 May 2026"))
            put("B-101", MaintenanceRecord("B-101", false, 1000, "10 May 2026"))
            put("B-102", MaintenanceRecord("B-102", true, 1000, "10 May 2026"))
        }
    }

    val complaintsRef = database.getReference("complaints")
    val noticesRef = database.getReference("announcements")

    LaunchedEffect(Unit) {
        noticesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                noticesStateList.clear()
                for (child in snapshot.children) {
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val desc = child.child("description").getValue(String::class.java) ?: ""
                    val time = child.child("time").getValue(String::class.java) ?: ""
                    noticesStateList.add(Triple(title, desc, time))
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    LaunchedEffect(Unit) {
        complaintsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaints.clear()
                for (child in snapshot.children) {
                    val complaint = child.getValue(Complaint::class.java)
                    if (complaint != null) complaints.add(complaint)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    BackHandler(enabled = currentScreen != "Login" && currentScreen != "Dashboard") {
        currentScreen = "Dashboard"
    }

    when (currentScreen) {
        "Dashboard" -> DashboardScreen(
            isAdmin = isAdmin,
            bookings = bookings,
            onMenuClick = { currentScreen = it },
            onLoginClick = {
                if (isAdmin) {
                    isAdmin = false
                } else {
                    currentScreen = "AdminLogin"
                }
            },
            maintenanceRecords = maintenanceRecords,
            complaints = complaints,
            notices = noticesStateList
        )

        "AdminLogin" -> LoginScreen(
            onLoginSuccess = {
                isAdmin = true
                currentScreen = "Dashboard"
            },
            onBack = { currentScreen = "Dashboard" }
        )

        "Maintenance" -> MaintenanceScreen(
            isAdmin = isAdmin,
            maintenanceRecords = maintenanceRecords,
            onBack = { currentScreen = "Dashboard" }
        )

        "Complaints" -> ComplaintScreen(
            isAdmin = isAdmin,
            complaints = complaints,
            complaintsRef = complaintsRef,
            onBack = { currentScreen = "Dashboard" }
        )

        "Parking" -> ParkingScreen(
            onBack = { currentScreen = "Dashboard" }
        )

        "Announcements" -> AnnouncementScreen(
            isAdmin = isAdmin,
            notices = noticesStateList,
            noticesRef = noticesRef,
            onBack = { currentScreen = "Dashboard" }
        )

        "Contacts" -> ContactsScreen(
            onBack = { currentScreen = "Dashboard" }
        )

        "Rules" -> RulesScreen(
            onBack = { currentScreen = "Dashboard" }
        )

        "HallBooking" -> HallBookingScreen(
            bookings = bookings,
            onBack = { currentScreen = "Dashboard" }
        )

        else -> FeatureInDevelopment(
            screenName = currentScreen,
            onBack = { currentScreen = "Dashboard" }
        )
    }
}

// 2. Enhanced Dashboard with Statistics
@Composable
fun DashboardScreen(
    isAdmin: Boolean,
    bookings: List<Booking>,
    onMenuClick: (String) -> Unit,
    onLoginClick: () -> Unit,
    maintenanceRecords: Map<String, MaintenanceRecord>,
    complaints: List<Complaint>,
    notices: List<Triple<String, String, String>>
) {
    val menuItems = listOf(
        MenuItem("Maintenance", Icons.Default.AccountBalance, "Check Dues", Color(0xFF4CAF50)),
        MenuItem("Parking", Icons.Default.DirectionsCar, "View Slot", Color(0xFF2196F3)),
        MenuItem("Announcements", Icons.Default.Notifications, "Updates", Color(0xFFFF9800)),
        MenuItem("Complaints", Icons.Default.ReportProblem, "File Issue", Color(0xFFF44336)),
        MenuItem("HallBooking", Icons.Default.Apartment, "Book Venue", Color(0xFF00BCD4)),
        MenuItem("Contacts", Icons.Default.ContactPhone, "Emergency", Color(0xFF9C27B0)),
        MenuItem("Rules", Icons.Default.Gavel, "Bylaws", Color(0xFF795548))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6200EE), Color(0xFF9C27B0))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Golden Nest",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Society Management",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Admin Mode",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            AdminStatsSection(maintenanceRecords, complaints)
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(menuItems) { item ->
                EnhancedMenuCard(item) { onMenuClick(item.title) }
            }
        }

        TextButton(
            onClick = onLoginClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        ) {
            Icon(
                if (isAdmin) Icons.Default.Logout else Icons.Default.Login,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isAdmin) "Logout" else "Admin Login",
                color = Color(0xFF6200EE),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AdminStatsSection(
    maintenanceRecords: Map<String, MaintenanceRecord>,
    complaints: List<Complaint>
) {
    val pendingPayments = maintenanceRecords.values.count { !it.isPaid }
    val pendingComplaints = complaints.count { it.status == "Pending" }
    val totalRevenue = maintenanceRecords.values.filter { it.isPaid }.sumOf { it.amount }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            title = "Pending",
            value = "$pendingPayments",
            subtitle = "Payments",
            icon = Icons.Default.PendingActions,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Active",
            value = "$pendingComplaints",
            subtitle = "Complaints",
            icon = Icons.Default.BugReport,
            color = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "₹${totalRevenue / 1000}K",
            value = "",
            subtitle = "Collected",
            icon = Icons.Default.AttachMoney,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            if (value.isNotEmpty()) {
                Text(
                    value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EnhancedMenuCard(item: MenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = item.color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = item.color,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Text(
                item.subtitle,
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 3. Enhanced Maintenance Screen with Admin Controls
@Composable
fun MaintenanceScreen(
    isAdmin: Boolean,
    maintenanceRecords: MutableMap<String, MaintenanceRecord>,
    onBack: () -> Unit
) {
    var unitSearch by remember { mutableStateOf("") }
    var showPaymentOptions by remember { mutableStateOf(false) }

    val isValidFormat = unitSearch.matches(Regex("^[AB]-1[0-9]{2}$|^[AB]-[1-9][0-9]{2}$"))
    val record = if (isValidFormat) maintenanceRecords[unitSearch] else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Surface(
            color = Color(0xFF6200EE),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "Maintenance Tracker",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = unitSearch,
                onValueChange = {
                    unitSearch = it.uppercase()
                    showPaymentOptions = false
                },
                label = { Text("Search Unit") },
                placeholder = { Text("e.g. A-101") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            if (unitSearch.isNotEmpty() && !isValidFormat) {
                Text(
                    "Format: A-101, A-102 ... B-101, B-102 ...",
                    color = Color(0xFFF44336),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            if (record != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (record.isPaid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Unit $unitSearch", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Surface(
                                color = if (record.isPaid) Color(0xFF4CAF50) else Color(0xFFF44336),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (record.isPaid) "PAID" else "PENDING",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow("Amount", "₹${record.amount}")
                        InfoRow("Due Date", record.dueDate)

                        if (!record.isPaid && !isAdmin) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showPaymentOptions = !showPaymentOptions },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (showPaymentOptions) "Hide QR Codes" else "Pay via QR")
                            }
                        }

                        if (isAdmin && !record.isPaid) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { maintenanceRecords[unitSearch] = record.copy(isPaid = true) },

                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark as Paid")
                            }
                        }

                        if (isAdmin && record.isPaid) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { maintenanceRecords[unitSearch] = record.copy(isPaid = false) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark as Unpaid")
                            }
                        }
                    }
                }

                if (showPaymentOptions && !record.isPaid) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Scan to Pay Maintenance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DummyQRCode("Google Pay")
                        DummyQRCode("PhonePe")
                    }
                }
            }

            if (isAdmin && record == null && unitSearch.isEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                AdminSummaryCard(maintenanceRecords)
            }
        }
    }
}

@Composable
fun DummyQRCode(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(2.dp, Color.LightGray, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.QrCode2,
                contentDescription = "QR Code",
                modifier = Modifier.size(80.dp),
                tint = Color.Black
            )
        }
        Text(
            label,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun AdminSummaryCard(records: Map<String, MaintenanceRecord>) {
    val totalPaid = records.values.count { it.isPaid }
    val totalPending = records.values.count { !it.isPaid }
    val totalAmount = records.values.filter { it.isPaid }.sumOf { it.amount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = null,
                    tint = Color(0xFF6200EE)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Collection Summary",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow("Total Units", "80")
            InfoRow("Paid", "$totalPaid")
            InfoRow("Pending", "$totalPending")
            InfoRow("Collected", "₹$totalAmount")
        }
    }
}

// 4. Enhanced Complaint Screen
@Composable
fun ComplaintScreen(
    isAdmin: Boolean,
    complaints: MutableList<Complaint>,
    complaintsRef: com.google.firebase.database.DatabaseReference,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Surface(
            color = Color(0xFF6200EE),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        "Complaints",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (isAdmin) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("All ${complaints.size}") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Pending ${complaints.count { it.status == "Pending" }}") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Resolved ${complaints.count { it.status == "Resolved" }}") }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (complaints.isEmpty()) {
                EmptyComplaintsView()
            } else {
                val filteredComplaints = when (selectedTab) {
                    1 -> complaints.filter { it.status == "Pending" }
                    2 -> complaints.filter { it.status == "Resolved" }
                    else -> complaints
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredComplaints) { complaint ->
                        ComplaintCard(complaint, isAdmin) { updatedComplaint ->
                            val index = complaints.indexOfFirst { it.id == complaint.id }
                            if (index != -1) {
                                complaints[index] = updatedComplaint
                                complaintsRef.child(updatedComplaint.id).setValue(updatedComplaint)
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFF6200EE)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Complaint", tint = Color.White)
            }
        }
    }

    if (showDialog) {
        NewComplaintDialog(
            onDismiss = { showDialog = false },
            onSubmit = { unit, description, category ->
                val newComplaintId = complaintsRef.push().key ?: return@NewComplaintDialog
                val newComplaint = Complaint(
                    id = newComplaintId,
                    unit = unit,
                    description = description,
                    status = "Pending",
                    timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()),
                    category = category
                )
                complaintsRef.child(newComplaintId).setValue(newComplaint)
                showDialog = false
            }
        )
    }
}
@Composable
fun EmptyComplaintsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF4CAF50).copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Complaints",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Text(
                "Tap + to file a new complaint",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ComplaintCard(
    complaint: Complaint,
    isAdmin: Boolean,
    onStatusChange: (Complaint) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF6200EE)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Unit ${complaint.unit}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = getCategoryColor(complaint.category).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            complaint.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = getCategoryColor(complaint.category),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    color = if (complaint.status == "Pending") Color(0xFFFF9800).copy(alpha = 0.1f)
                    else Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        complaint.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = if (complaint.status == "Pending") Color(0xFFFF9800) else Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(complaint.description, fontSize = 14.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                complaint.timestamp,
                fontSize = 11.sp,
                color = Color.Gray
            )

            if (isAdmin && complaint.status == "Pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onStatusChange(complaint.copy(status = "Resolved"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Resolved")
                }
            }
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Plumbing" -> Color(0xFF2196F3)
        "Electrical" -> Color(0xFFFF9800)
        "Noise" -> Color(0xFFF44336)
        "Maintenance" -> Color(0xFF4CAF50)
        "Security" -> Color(0xFF9C27B0)
        else -> Color.Gray
    }
}

@Composable
fun NewComplaintDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var unit by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Maintenance") }
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf("Maintenance", "Plumbing", "Electrical", "Noise", "Security", "Other")
    val isValid = unit.matches(Regex("^[AB]-([1-9]|10)0[1-4]$")) && description.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File a Complaint", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = unit.uppercase(),
                    onValueChange = { unit = it.uppercase() },
                    label = { Text("Your Unit") },
                    placeholder = { Text("e.g. A-101") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = unit.isNotEmpty() && !unit.matches(Regex("^[AB]-([1-9]|10)0[1-4]$"))
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Describe the issue...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(unit, description, selectedCategory) },
                enabled = isValid
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Continue with remaining screens (Parking, Announcements, Contacts, Rules, Login, HallBooking, FeatureInDevelopment)
// Due to length constraints, I'm including the Hall Booking screen which had the main error:

@Composable
fun ParkingScreen(onBack: () -> Unit) {
    var unitSearch by remember { mutableStateOf("") }
    var showAllSlots by remember { mutableStateOf(false) }

    val isValidFormat = unitSearch.matches(Regex("^[AB]-([1-9]|10)0[1-4]$"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Surface(
            color = Color(0xFF2196F3),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "Parking Management",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = unitSearch.uppercase(),
                onValueChange = { unitSearch = it.uppercase() },
                label = { Text("Search Unit") },
                placeholder = { Text("e.g. A-101") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    focusedLabelColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isValidFormat) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF2196F3).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.LocalParking,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Unit $unitSearch",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Slot: ${unitSearch.replace("-", "")}",
                                fontSize = 20.sp,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Ground Floor",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { showAllSlots = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View All Parking Slots (80)")
            }
        }
    }

    if (showAllSlots) {
        ModalBottomSheet(onDismissRequest = { showAllSlots = false }) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .padding(16.dp)
            ) {
                Text(
                    "All Parking Slots",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn {
                    listOf("A", "B").forEach { wing ->
                        item {
                            Text(
                                "Wing $wing",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        (1..10).forEach { floor ->
                            (1..4).forEach { flat ->
                                val unit = "$wing-${floor}0$flat"
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Home,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = Color(0xFF2196F3)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Unit $unit",
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text(
                                                "${unit.replace("-", "")}",
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 6. Announcements Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementScreen(
    isAdmin: Boolean,
    notices: List<Triple<String, String, String>>,
    noticesRef: com.google.firebase.database.DatabaseReference,
    onBack: () -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var contentInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header with Back Button
        Surface(
            color = Color(0xFFFF9800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "Announcements",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Admin control section at the top of the feed list
            if (isAdmin) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Publish Broadcast Notice", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text("Notice Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = contentInput,
                                onValueChange = { contentInput = it },
                                label = { Text("Notice Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (titleInput.isNotBlank() && contentInput.isNotBlank()) {
                                        // Send to Firebase Realtime Database (Kept exactly as it was)
                                        val newNoticeId = noticesRef.push().key ?: ""
                                        val noticeData = mapOf(
                                            "title" to titleInput,
                                            "description" to contentInput,
                                            "time" to "Just Now"
                                        )
                                        noticesRef.child(newNoticeId).setValue(noticeData)

                                        //Trigger the push notification to everyone using your text inputs
                                        coroutineScope.launch {
                                            sendNotificationToTopic(title = titleInput, messageBody = contentInput)
                                        }

                                        // Clear inputs (Kept exactly as it was)
                                        titleInput = ""
                                        contentInput = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Broadcast Now", color = Color.White)
                            }
                        }
                    }
                }
            }

            items(notices) { (title, notice, time) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Surface(
                            color = Color(0xFFFF9800).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(notice, fontSize = 14.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(time, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}@Composable
fun ContactsScreen(onBack: () -> Unit) {
    val adminContacts = listOf(
        Triple("Society Chairman", "+91 98765 43210", Icons.Default.Person),
        Triple("Secretary", "+91 98765 11223", Icons.Default.Description),
        Triple("Treasurer", "+91 98765 55667", Icons.Default.AccountBalance),
        Triple("Security Desk", "Ext: 101", Icons.Default.Security),
        Triple("Maintenance Office", "+91 98765 99887", Icons.Default.Build)
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Surface(color = Color(0xFF9C27B0), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Emergency Contacts", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(adminContacts) { (name, phone, icon) ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFF9C27B0).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(phone, color = Color.Gray, fontSize = 14.sp)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RulesScreen(onBack: () -> Unit) {
    val societyRules = listOf(
        "Parking is only allowed in designated slots.",
        "Quiet hours are strictly observed from 10 PM to 7 AM.",
        "Garbage must be segregated into dry and wet waste.",
        "No major construction/renovation during weekends.",
        "Pets must be leashed in common society areas.",
        "Maintenance dues must be cleared by the 10th of every month.",
        "Visitors must register at the security desk.",
        "Common areas should be kept clean at all times."
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Surface(color = Color(0xFF795548), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Society Bylaws", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(societyRules) { index, rule ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Surface(color = Color(0xFF795548).copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", fontWeight = FontWeight.Bold, color = Color(0xFF795548), fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(rule, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onBack: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Surface(color = Color(0xFF6200EE), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Admin Login", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF6200EE))
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(value = username, onValueChange = { username = it; error = false }, label = { Text("Username") }, leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = password, onValueChange = { password = it; error = false }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }, trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null) } }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))

            if (error) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Invalid credentials. Try admin/nest123", color = Color(0xFFF44336), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { if (username == "admin" && password == "nest123") { onLoginSuccess() } else { error = true } }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)), shape = RoundedCornerShape(12.dp)) {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Demo Credentials:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Username: admin", fontSize = 12.sp, color = Color.Gray)
                    Text("Password: nest123", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun HallBookingScreen(
    bookings: MutableList<Booking>,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedHall by remember { mutableStateOf("Hall A") }
    var bookingDate by remember { mutableStateOf("") }
    var durationHours by remember { mutableStateOf(1f) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val isAlreadyBooked = bookings.any { it.hallName == selectedHall && it.date == bookingDate }
    val hourlyRate = if (selectedHall == "Hall A") 1200 else 800
    val totalAmount = (hourlyRate * durationHours.toInt())

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).verticalScroll(scrollState).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Community Booking", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Choose Venue", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Hall A", "Hall B").forEach { hall ->
                        FilterChip(selected = selectedHall == hall, onClick = { selectedHall = hall }, label = { Text(hall) }, leadingIcon = if (selectedHall == hall) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(value = bookingDate, onValueChange = { bookingDate = it }, label = { Text("Date (DD/MM/YYYY)") }, modifier = Modifier.fillMaxWidth(), isError = isAlreadyBooked, supportingText = { if (isAlreadyBooked) { Text("This slot is already booked for $selectedHall!", color = Color.Red) } })

                Spacer(modifier = Modifier.height(24.dp))

                Text("Duration: ${durationHours.toInt()} Hours", fontWeight = FontWeight.Medium)
                Slider(value = durationHours, onValueChange = { durationHours = it }, valueRange = 1f..12f, steps = 10, colors = SliderDefaults.colors(thumbColor = Color(0xFF6200EE)))

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8F9FA)).padding(16.dp)) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rate ($selectedHall)", color = Color.Gray)
                            Text("₹$hourlyRate / hr")
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total to Pay", fontWeight = FontWeight.Bold)
                            Text("₹$totalAmount", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.d   p))

                Button(onClick = { bookings.add(Booking(selectedHall, bookingDate, durationHours.toInt())); showSuccessDialog = true }, modifier = Modifier.fillMaxWidth(), enabled = !isAlreadyBooked && bookingDate.length >= 8, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))) {
                    Text(if (isAlreadyBooked) "Slot Unavailable" else "Confirm & Pay")
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(onDismissRequest = { showSuccessDialog = false }, title = { Text("Booking Successful") }, text = { Text("You have booked $selectedHall for $bookingDate. Our manager will contact you for details.") }, confirmButton = { TextButton(onClick = { showSuccessDialog = false; onBack() }) { Text("Done") } })
    }
}

@Composable
fun FeatureInDevelopment(screenName: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("$screenName Coming Soon!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("This feature is under development", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))) {
            Text("Go Back")
        }
    }
}

suspend fun sendNotificationToTopic(title: String, messageBody: String) {
    withContext(Dispatchers.IO) {
        try {
            val projectId = "goldennest-1576e"
            val clientEmail = "firebase-adminsdk-fbsvc@goldennest-1576e.iam.gserviceaccount.com"

            val rawPrivateKey = """
                MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDLR/KB0VkT36RB
                ZUizXwSSIk5UuZWjYjgds4qqgIxsiP6ar0xenqpiUp/xkLe3juKHZa2Bt1XAVguZ
                fDu56ko7hpGKaztqrzZp8NK+d5XW5j2Q0ZiI1eRQyLi4zlj4SOFu3I0pvNL1Hjsw
                R6LRNXdpJxcnWQVB9hgWVYEKVef/Lyr9tFJZl+1VBh4t6DyBlCGLK2TtevVXu0Kk
                aKyxf43yhwxgSLq/yeQi6KfHZx2FzdbcfmAC7u1hkmNtCmXFRzQJ8f1GuGU2SVMJ
                eSM7B214zKAydi2ZmGisHSujiCgfU8c3/1ns6jU7x8aLRkCPH3YwaFmxq+7A/tsu
                wC80jOQfAgMBAAECggEAVdsHzhN8w1uH9MTR3UxK0muh2L/fNvfTSpwbe2K6rFZ5
                7hwOrcHmpYtPGUgth93VyCjGDMzBb3AICyXA1gdhnd16l9Mtb6Qb38fCQoagZvis
                VP9pJJXuDb6Q4iDoy8iASgHlrxScpsFDb2M6HZEu418KtOgww8isKLapPxPEXe6t
                E+OQ0rsz+tMRK4ATxJ56+74gsvjoUNS2C1TBosv3i+yrbc6lrNKEa5ZJccvoqU96
                Mvtnb3ioFyygoURIhn6k9NSQORyJ94k7SNM06E07yEFmXQF7wSQaJbCo+uH7Xroq
                EDarsMoRu0lf4ovHkw11undYiY9b9v/CrZjTxhERzQKBgQD458EoNnBPrdukLjE6
                5C67tyAoyAriDng/U2sKQjCNuVSGWAUk3yNCwBLsC/tnChRWFjYFsX0YJqJN2EIo
                6yj5o2VTU7YvQK4vByI4MRUrC+2w7PLIP5IhUhYrMEfhVCl0hF5bKlyiZbhXidaw
                RYQnRruwcZuVPSaFP1pDGO6kNQKBgQDRE0aRTwacHOIa+OahT4m9WyvbEOxTGtLd
                NhQttybW+aIuHAl9YfEOqKRv9hBKIxoiMo50SipBqe8xkG1E6hIP18NM2ZmX17Gd
                4QfD8cYDR5xVYNg7ZfdIq+mmPN5zUdS97+aQnzdDqfAy9+AvJ4UrOQAD5KoRXMfv
                obkCZyUJgwKBgQDaXbhAJ0JxJP5FE6FtITM+zHISVS33FOq5491MqrUeITHeiuo4
                ZurwbMItBHYS9+zPebz7UbOFtJ8/3DJu46CXIpqKeC6lVgF6kK+czLLdiSGGztpQ
                hDUXtoRsb3cgYE3eQyLkqE2My+DmK6+GBfbi3lNbgNAWJgfdbq8pnn3+4QKBgQCr
                IzkXRnWloereozC9iAq6af+bAN5BJGrIUjTwOsRbIdnEHoUSEfKsyhRX5TXuSY9h
                wLpkrDSsrToy+wLGb0c7mqStqkyUQyWLkyC1Hpm0I5DvPiehb1ScMsQ9pAN+p9QZ
                aV70sK8e/uK0BbyHPLU/7Rd7GFixf53VJC75rY5emwKBgQDDNXeL65MqCo8QnL7a
                0FC1ERk+Ir7TawWfu9fgsZ3e7q9ky4EIQcpEDEhMdo/C6pvr5IicIRtETO3SvN+0
                vIzn9U04tjSR46QAEmXCZgLW1ePZQ9nVcmypVdXajIyTXTvWF9tZByG15KQs6f2m
                Cue4iUTXKQumBFpGZUGNKnUlvw==
            """.trimIndent().replace("\n", "").replace(" ", "")

            val now = Date()
            val expiry = Date(now.time + 3600 * 1000)

            val keyBytes = Base64.decode(rawPrivateKey, Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)

            val accessToken = Jwts.builder()
                .setIssuer(clientEmail)
                .setAudience("https://oauth2.googleapis.com/token")
                .setExpiration(expiry)
                .setIssuedAt(now)
                .claim("scope", "https://www.googleapis.com/auth/firebase.messaging")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact()

            val url = URL("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.doOutput = true

            val jsonPayload = """
                {
                  "message": {
                    "topic": "announcements",
                    "notification": {
                      "title": "$title",
                      "body": "$messageBody"
                    }
                  }
                }
            """.trimIndent()

            conn.outputStream.use { os ->
                val input = jsonPayload.toByteArray(charset("utf-8"))
                os.write(input, 0, input.size)
            }

            val responseCode = conn.responseCode
            val responseMessage = conn.inputStream.bufferedReader().use { it.readText() }
            Log.d("Notification", "HTTP Status: $responseCode, Response: $responseMessage")

        } catch (e: Exception) {
            Log.e("Notification", "Error broadcasting message", e)
        }
    }
}