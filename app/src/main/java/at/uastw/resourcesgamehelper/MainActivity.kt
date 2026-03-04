package at.uastw.resourcesgamehelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import at.uastw.resourcesgamehelper.ui.theme.ResourcesGameHelperTheme
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ResourcesGameHelperTheme {
                ResourcesGameHelperApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesGameHelperApp(viewModel: MineViewModel = viewModel()) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val mines by viewModel.mines.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val context = LocalContext.current
    val maintenanceManager = remember { MaintenanceManager(context) }
    
    var selectedMine by remember { mutableStateOf<Mine?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val timeTicker by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }

    LaunchedEffect(mines) {
        mines.forEach { maintenanceManager.syncWithApi(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchMines()
    }

    if (showBottomSheet && selectedMine != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            val history = remember(selectedMine, timeTicker) { 
                maintenanceManager.getHistory(selectedMine!!.mineID) 
            }
            MineDetailContent(selectedMine!!, timeTicker, history)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                if (currentDestination == AppDestinations.HOME) {
                    FloatingActionButton(onClick = { viewModel.fetchMines() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestinations.HOME -> {
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (mines.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No mines found. Press refresh to load.")
                            }
                        } else {
                            MineList(mines = mines, ticker = timeTicker, onMineClick = {
                                selectedMine = it
                                showBottomSheet = true
                            })
                        }
                    }
                    AppDestinations.FAVORITES -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Favorites Screen") }
                    AppDestinations.PROFILE -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Profile Screen") }
                }
            }
        }
    }
}

@Composable
fun MineList(mines: List<Mine>, ticker: Long, onMineClick: (Mine) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mines) { mine ->
            MineItem(mine = mine, ticker = ticker, onClick = { onMineClick(mine) })
        }
    }
}

@Composable
fun MineItem(mine: Mine, ticker: Long, onClick: () -> Unit) {
    val iconUrl = GameData.getIconUrlForResource(mine.resourceID)
    val currentOutput = MineCalculator.calculateCurrentHourlyOutput(mine)
    val outputFormatter = remember { DecimalFormat("#,##0.000") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconUrl != null) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = mine.resourceName,
                    modifier = Modifier.size(40.dp).padding(end = 12.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = mine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Output: ${outputFormatter.format(currentOutput)} m³/h",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val currentCondition = MineCalculator.calculateCurrentCondition(mine.lastmaintenance)
                Text(
                    text = String.format("%.4f%%", currentCondition * 100),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentCondition < 0.5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = "Condition", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun MineDetailContent(mine: Mine, ticker: Long, history: List<Long>) {
    val totalEarned = MineCalculator.calculateTotalEarningsWithHistory(mine, history)
    val currentHourlyOutput = MineCalculator.calculateCurrentHourlyOutput(mine)
    val currentHourlyMoney = MineCalculator.calculateCurrentHourlyEarnings(mine)
    val currentLoss = MineCalculator.calculateCurrentHourlyLoss(mine)
    
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }
    val outputFormatter = remember { DecimalFormat("#,##0.000") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 48.dp)
    ) {
        Text(text = mine.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = "Resource: ${mine.resourceName}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Total Earnings (History Incl.)", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = currencyFormatter.format(totalEarned),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailStat(label = "Output", value = "${outputFormatter.format(currentHourlyOutput)} m³/h")
            DetailStat(label = "Current Income", value = "${currencyFormatter.format(currentHourlyMoney)}/h")
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailStat(label = "Current Loss", value = "${currencyFormatter.format(currentLoss)}/h", color = MaterialTheme.colorScheme.error)
            DetailStat(label = "HQ Boost", value = "x${mine.HQboost}")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Condition History (%)", style = MaterialTheme.typography.titleMedium)
        PerformanceHistoryChart(mine, ticker, history, chartType = ChartType.CONDITION)
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Income History (€/h)", style = MaterialTheme.typography.titleMedium)
        PerformanceHistoryChart(mine, ticker, history, chartType = ChartType.INCOME)

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Lost Income History (€/h)", style = MaterialTheme.typography.titleMedium)
        PerformanceHistoryChart(mine, ticker, history, chartType = ChartType.LOSS)
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun newSimpleDate(seconds: Long): String {
    return SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(seconds * 1000))
}

@Composable
fun DetailStat(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

enum class ChartType { CONDITION, INCOME, LOSS }

@Composable
fun PerformanceHistoryChart(mine: Mine, ticker: Long, history: List<Long>, chartType: ChartType) {
    val now = ticker / 1000
    val buildDate = mine.builddate
    
    // Relevant history points after build date
    val points = history.filter { it > buildDate && it < now }.sorted().toMutableList()
    
    val totalElapsed = (now - buildDate).coerceAtLeast(1)
    
    val dateFormat = remember { SimpleDateFormat("dd.MM.yy", Locale.getDefault()) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }
    
    val maxEarnings = MineCalculator.calculateMaxHourlyEarnings(mine)
    val maxVal = when(chartType) {
        ChartType.CONDITION -> 1.0
        ChartType.INCOME -> maxEarnings
        ChartType.LOSS -> maxEarnings
    }

    Row(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            if (chartType == ChartType.CONDITION) {
                Text("100%", style = MaterialTheme.typography.labelSmall)
                Text("50%", style = MaterialTheme.typography.labelSmall)
                Text("0%", style = MaterialTheme.typography.labelSmall)
            } else {
                Text(currencyFormatter.format(maxVal), style = MaterialTheme.typography.labelSmall)
                Text(currencyFormatter.format(maxVal / 2), style = MaterialTheme.typography.labelSmall)
                Text("0 €", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    drawLine(Color.LightGray.copy(alpha = 0.3f), start = Offset(0f, 0f), end = Offset(width, 0f))
                    drawLine(Color.LightGray.copy(alpha = 0.3f), start = Offset(0f, height/2), end = Offset(width, height/2))
                    
                    drawLine(Color.Gray, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)
                    drawLine(Color.Gray, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 2f)
                    
                    val path = Path()
                    var lastX = 0f
                    var lastTime = buildDate

                    // Start at Build (100% or 0 loss)
                    if (chartType == ChartType.LOSS) path.moveTo(0f, height) else path.moveTo(0f, 0f)

                    for (maintTime in points) {
                        val x = ((maintTime - buildDate).toDouble() / totalElapsed).toFloat() * width
                        val decay = (maintTime - lastTime) * MineCalculator.DECAY_RATE_PER_SECOND
                        
                        if (chartType == ChartType.LOSS) {
                            path.lineTo(x, height - (decay.coerceIn(0.0, 1.0).toFloat()) * height)
                            path.lineTo(x, height) // Jump to zero loss
                        } else {
                            path.lineTo(x, (decay.coerceIn(0.0, 1.0).toFloat()) * height)
                            path.lineTo(x, 0f) // Jump to 100%
                        }
                        lastX = x
                        lastTime = maintTime
                    }

                    // Final segment to Now
                    val decayNow = (now - lastTime) * MineCalculator.DECAY_RATE_PER_SECOND
                    if (chartType == ChartType.LOSS) {
                        path.lineTo(width, height - (decayNow.coerceIn(0.0, 1.0).toFloat()) * height)
                    } else {
                        path.lineTo(width, (decayNow.coerceIn(0.0, 1.0).toFloat()) * height)
                    }
                    
                    val strokeColor = when(chartType) {
                        ChartType.CONDITION -> Color(0xFF2196F3)
                        ChartType.INCOME -> Color(0xFF4CAF50)
                        ChartType.LOSS -> Color(0xFFF44336)
                    }
                    drawPath(path = path, color = strokeColor, style = Stroke(width = 4f))
                    
                    // Draw vertical markers for all maintenance events
                    for (maintTime in points) {
                        val x = ((maintTime - buildDate).toDouble() / totalElapsed).toFloat() * width
                        drawLine(
                            color = Color.Green.copy(alpha = 0.2f),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f
                        )
                    }
                    
                    drawLine(Color.Red, start = Offset(width, 0f), end = Offset(width, height), strokeWidth = 3f)
                }
            }
            
            Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                Text(
                    text = dateFormat.format(Date(buildDate * 1000)),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Text(
                    text = "Teď",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Mines", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}
