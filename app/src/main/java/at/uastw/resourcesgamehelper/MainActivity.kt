package at.uastw.resourcesgamehelper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

enum class MineSortType(val labelRes: Int) {
    RESOURCES(R.string.sort_resources),
    QUALITY(R.string.sort_quality),
    CONDITION(R.string.sort_condition),
    DEFENSE(R.string.sort_defense)
}

enum class FactorySortType(val labelRes: Int) {
    PROFIT(R.string.sort_profit),
    REVENUE(R.string.sort_revenue),
    COST(R.string.sort_cost)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesGameHelperApp(viewModel: MineViewModel = viewModel()) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val mines by viewModel.mines.collectAsState()
    val factoryProfits by viewModel.factoryProfits.collectAsState()
    val bestHqLocation by viewModel.bestHqLocation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val context = LocalContext.current
    val maintenanceManager = remember { MaintenanceManager(context) }
    
    var selectedMinePos by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var mineSort by rememberSaveable { mutableStateOf(MineSortType.RESOURCES) }
    var factorySort by rememberSaveable { mutableStateOf(FactorySortType.PROFIT) }
    var isDescending by rememberSaveable { mutableStateOf(true) }
    
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val expandedResources = remember { mutableStateMapOf<String, Boolean>() }

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
        viewModel.fetchFactories()
    }

    if (showBottomSheet && selectedMinePos != null) {
        val currentMine = mines.find { it.lat == selectedMinePos!!.first && it.lon == selectedMinePos!!.second }
        if (currentMine != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                var history by remember(currentMine, timeTicker) { 
                    mutableStateOf(maintenanceManager.getHistory(currentMine)) 
                }
                
                MineDetailContent(
                    currentMine, 
                    timeTicker, 
                    history,
                    onRemoveMaint = { ts -> 
                        maintenanceManager.removePoint(currentMine, ts)
                        history = maintenanceManager.getHistory(currentMine)
                    },
                    onClearAll = {
                        maintenanceManager.clearHistory(currentMine)
                        history = emptyList()
                    }
                )
            }
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = stringResource(it.labelRes)) },
                    label = { Text(stringResource(it.labelRes)) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Surface(shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(currentDestination.labelRes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { 
                                if (currentDestination == AppDestinations.HOME) viewModel.fetchMines() 
                                else if (currentDestination == AppDestinations.FACTORIES) viewModel.fetchFactories()
                                else viewModel.calculateBestHq()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                            }
                        }
                        if (currentDestination == AppDestinations.HOME || currentDestination == AppDestinations.FACTORIES) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    val currentSortLabel = if (currentDestination == AppDestinations.HOME) 
                                        stringResource(mineSort.labelRes) else stringResource(factorySort.labelRes)
                                    
                                    AssistChip(
                                        onClick = { sortMenuExpanded = true },
                                        label = { Text(stringResource(R.string.sort_label, currentSortLabel)) },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                                    )
                                    DropdownMenu(
                                        expanded = sortMenuExpanded,
                                        onDismissRequest = { sortMenuExpanded = false }
                                    ) {
                                        if (currentDestination == AppDestinations.HOME) {
                                            MineSortType.entries.forEach { type ->
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(type.labelRes)) },
                                                    onClick = {
                                                        mineSort = type
                                                        sortMenuExpanded = false
                                                    }
                                                )
                                            }
                                        } else {
                                            FactorySortType.entries.forEach { type ->
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(type.labelRes)) },
                                                    onClick = {
                                                        factorySort = type
                                                        sortMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.weight(1f))
                                
                                IconButton(
                                    onClick = { isDescending = !isDescending },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = if (isDescending) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                        contentDescription = stringResource(R.string.sort_order)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestinations.HOME -> {
                        if (isLoading && mines.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (mines.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_mines_found))
                            }
                        } else {
                            if (mineSort == MineSortType.RESOURCES) {
                                val groupedMines = remember(mines, isDescending) {
                                    val comparator = if (isDescending) reverseOrder<String>() else naturalOrder<String>()
                                    mines.groupBy { it.resourceName }.toSortedMap(comparator)
                                }
                                GroupedMineList(
                                    groupedMines = groupedMines,
                                    expandedState = expandedResources,
                                    ticker = timeTicker,
                                    onMineClick = {
                                        selectedMinePos = Pair(it.lat, it.lon)
                                        showBottomSheet = true
                                    }
                                )
                            } else {
                                val sortedMines = remember(mines, mineSort, isDescending, timeTicker) {
                                    val comparator = when (mineSort) {
                                        MineSortType.QUALITY -> compareBy<Mine> { it.quality }
                                        MineSortType.CONDITION -> compareBy<Mine> { MineCalculator.calculateCurrentCondition(it.lastmaintenance) }
                                        MineSortType.DEFENSE -> compareBy<Mine> { it.def1 + it.def2 + it.def3 }
                                        else -> compareBy<Mine> { it.resourceName }
                                    }
                                    if (isDescending) mines.sortedWith(comparator.reversed()) else mines.sortedWith(comparator)
                                }
                                MineList(mines = sortedMines, ticker = timeTicker, onMineClick = {
                                    selectedMinePos = Pair(it.lat, it.lon)
                                    showBottomSheet = true
                                })
                            }
                        }
                    }
                    AppDestinations.FACTORIES -> {
                        if (isLoading && factoryProfits.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val sortedFactories = remember(factoryProfits, factorySort, isDescending) {
                                val comparator = when (factorySort) {
                                    FactorySortType.PROFIT -> compareBy<FactoryProfit> { it.profitPerHour }
                                    FactorySortType.REVENUE -> compareBy<FactoryProfit> { it.revenuePerHour }
                                    FactorySortType.COST -> compareBy<FactoryProfit> { it.costPerHour }
                                }
                                if (isDescending) factoryProfits.sortedWith(comparator.reversed()) else factoryProfits.sortedWith(comparator)
                            }
                            FactoryList(sortedFactories)
                        }
                    }
                    AppDestinations.COSTS -> NewMinesCostsScreen(mines)
                    AppDestinations.HQ -> HqScreen(bestHqLocation, isLoading, onCalculate = { viewModel.calculateBestHq() })
                    AppDestinations.PROFILE -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.tab_profile)) }
                }
            }
        }
    }
}

@Composable
fun GroupedMineList(
    groupedMines: Map<String, List<Mine>>,
    expandedState: MutableMap<String, Boolean>,
    ticker: Long,
    onMineClick: (Mine) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groupedMines.forEach { (resourceName, mines) ->
            item(key = "header_$resourceName") {
                val isExpanded = expandedState[resourceName] ?: false
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedState[resourceName] = !isExpanded }
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$resourceName (${mines.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }
            }
            if (expandedState[resourceName] == true) {
                items(mines, key = { "${it.lat}_${it.lon}" }) { mine ->
                    MineItem(mine = mine, ticker = ticker, onClick = { onMineClick(mine) })
                }
            }
        }
    }
}

@Composable
fun NewMinesCostsScreen(mines: List<Mine>) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }
    val totalMinesCount = mines.size
    val costItems = remember(totalMinesCount) {
        GameData.items.filter { it.baseMineCost > 0 }.map { item ->
            val currentCost = item.baseMineCost + (item.baseMineCost * 0.02 * totalMinesCount)
            Triple(item, totalMinesCount, currentCost)
        }.sortedByDescending { it.third }
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(text = stringResource(R.string.total_mines_owned, totalMinesCount), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Text(text = stringResource(R.string.next_mine_price_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
        }
        items(costItems) { (item, _, cost) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (item.iconUrl != null) {
                        AsyncImage(model = item.iconUrl, contentDescription = null, modifier = Modifier.size(40.dp).padding(end = 12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(text = currencyFormatter.format(cost), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun HqScreen(result: Pair<Pair<Double, Double>?, Int>, isLoading: Boolean, onCalculate: () -> Unit) {
    val context = LocalContext.current
    val theoreticalMax = remember { HqCalculator.calculateMaxMinesInRadius() }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.calculating_hq))
        } else {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.theoretical_max_mines), style = MaterialTheme.typography.labelMedium)
                    Text(text = "$theoreticalMax", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.hq_spacing_desc), style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (result.first != null) {
                Text(text = stringResource(R.string.best_hq_location), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(R.string.mines_in_radius), style = MaterialTheme.typography.labelMedium)
                        Text(text = "${result.second}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        val coordText = String.format(Locale.US, "%.6f %.6f", result.first!!.first, result.first!!.second)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = stringResource(R.string.lat_label, result.first!!.first), style = MaterialTheme.typography.bodyMedium)
                                Text(text = stringResource(R.string.lon_label, result.first!!.second), style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("HQ", coordText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                            }) { Icon(Icons.Default.Share, contentDescription = stringResource(R.string.copied)) }
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.find_hq))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCalculate, modifier = Modifier.fillMaxWidth()) {
                Text(if (result.first == null) stringResource(R.string.find_hq) else stringResource(R.string.recalculate))
            }
        }
    }
}

@Composable
fun FactoryList(profits: List<FactoryProfit>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(profits) { profit -> FactoryItem(profit) }
    }
}

@Composable
fun FactoryItem(profit: FactoryProfit) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val iconUrl = GameData.getIconUrlForResource(profit.outputItemId)
            if (iconUrl != null) {
                AsyncImage(model = iconUrl, contentDescription = null, modifier = Modifier.size(48.dp).padding(end = 16.dp), contentScale = ContentScale.Fit)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profit.factoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.produces_label, profit.outputItemName), style = MaterialTheme.typography.bodySmall)
                Text(text = stringResource(R.string.profit_label, currencyFormatter.format(profit.profitPerHour)), color = if (profit.profitPerHour >= 0) Color(0xFF4CAF50) else Color.Red, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = stringResource(R.string.rev_short, currencyFormatter.format(profit.revenuePerHour)), style = MaterialTheme.typography.labelSmall)
                Text(text = stringResource(R.string.cost_short, currencyFormatter.format(profit.costPerHour)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MineList(mines: List<Mine>, ticker: Long, onMineClick: (Mine) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(mines, key = { "${it.lat}_${it.lon}" }) { mine -> MineItem(mine = mine, ticker = ticker, onClick = { onMineClick(mine) }) }
    }
}

@Composable
fun MineItem(mine: Mine, ticker: Long, onClick: () -> Unit) {
    val iconUrl = GameData.getIconUrlForResource(mine.resourceID)
    val currentOutput = MineCalculator.calculateCurrentHourlyOutput(mine)
    val outputFormatter = remember { DecimalFormat("#,##0.00") }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (iconUrl != null) {
                AsyncImage(model = iconUrl, contentDescription = null, modifier = Modifier.size(40.dp).padding(end = 12.dp), contentScale = ContentScale.Fit)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = mine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.output_label, outputFormatter.format(currentOutput)), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.unique_id_label, mine.lat, mine.lon), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                val currentCondition = MineCalculator.calculateCurrentCondition(mine.lastmaintenance)
                Text(text = String.format(Locale.US, "%.4f%%", currentCondition * 100), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (currentCondition < 0.5) Color.Red else Color.Unspecified)
                Text(text = stringResource(R.string.cond_short), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun MineDetailContent(mine: Mine, ticker: Long, history: List<Long>, onRemoveMaint: (Long) -> Unit, onClearAll: () -> Unit) {
    val totalEarned = MineCalculator.calculateTotalEarningsWithHistory(mine, history)
    val currentHourlyOutput = MineCalculator.calculateCurrentHourlyOutput(mine)
    val currentHourlyMoney = MineCalculator.calculateCurrentHourlyEarnings(mine)
    val currentLoss = MineCalculator.calculateCurrentHourlyLoss(mine)
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }
    val outputFormatter = remember { DecimalFormat("#,##0.000") }
    val fullDateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()).padding(bottom = 48.dp)) {
        Text(text = mine.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = stringResource(R.string.unique_id_label, mine.lat, mine.lon), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.total_earnings_est), style = MaterialTheme.typography.labelMedium)
                Text(text = currencyFormatter.format(totalEarned), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailStat(label = stringResource(R.string.current_output), value = "${outputFormatter.format(currentHourlyOutput)} m³/h")
            DetailStat(label = stringResource(R.string.current_income), value = "${currencyFormatter.format(currentHourlyMoney)}/h")
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailStat(label = stringResource(R.string.current_loss), value = "${currencyFormatter.format(currentLoss)}/h", color = Color.Red)
            DetailStat(label = "Boost", value = stringResource(R.string.hq_boost_label, mine.HQboost))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailStat(label = stringResource(R.string.sort_defense), value = "${mine.def1} / ${mine.def2} / ${mine.def3}")
            DetailStat(label = stringResource(R.string.sort_quality), value = "${(mine.quality * 100).toInt()}%")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = stringResource(R.string.chart_condition), style = MaterialTheme.typography.titleMedium)
        PerformanceHistoryChart(mine, ticker, history, chartType = ChartType.CONDITION)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = stringResource(R.string.chart_income), style = MaterialTheme.typography.titleMedium)
        PerformanceHistoryChart(mine, ticker, history, chartType = ChartType.INCOME)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = stringResource(R.string.chart_loss), style = MaterialTheme.typography.titleMedium)
        PerformanceHistoryChart(mine, ticker, history, chartType = ChartType.LOSS)
        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.maintenance_history), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClearAll) { Text(stringResource(R.string.clear_all), color = Color.Red) }
        }
        history.reversed().forEach { ts ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = fullDateFormat.format(Date(ts * 1000)), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { onRemoveMaint(ts) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
            }
        }
        if (history.isEmpty()) Text(text = stringResource(R.string.no_history), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DetailStat(label: String, value: String, color: Color = Color.Unspecified) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

enum class ChartType { CONDITION, INCOME, LOSS }

@Composable
fun PerformanceHistoryChart(mine: Mine, ticker: Long, history: List<Long>, chartType: ChartType) {
    val now = ticker / 1000
    val buildDate = mine.builddate
    val points = history.filter { it > buildDate && it < now }.sorted().toMutableList()
    val totalElapsed = (now - buildDate).coerceAtLeast(1)
    val dateFormat = remember { SimpleDateFormat("dd.MM.yy", Locale.getDefault()) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }
    val maxEarnings = MineCalculator.calculateMaxHourlyEarnings(mine)
    val maxVal = if (chartType == ChartType.CONDITION) 1.0 else maxEarnings
    Row(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Column(modifier = Modifier.fillMaxHeight().padding(end = 8.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
            if (chartType == ChartType.CONDITION) {
                Text("100%", style = MaterialTheme.typography.labelSmall)
                Text("0%", style = MaterialTheme.typography.labelSmall)
            } else {
                Text(currencyFormatter.format(maxVal), style = MaterialTheme.typography.labelSmall)
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
                    drawLine(Color.Gray, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)
                    drawLine(Color.Gray, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 2f)
                    val path = Path()
                    var lastTime = buildDate
                    if (chartType == ChartType.LOSS) path.moveTo(0f, height) else path.moveTo(0f, 0f)
                    for (maintTime in points) {
                        val x = ((maintTime - buildDate).toDouble() / totalElapsed).toFloat() * width
                        val decay = (maintTime - lastTime) * MineCalculator.DECAY_RATE_PER_SECOND
                        if (chartType == ChartType.LOSS) {
                            path.lineTo(x, height - (decay.coerceIn(0.0, 1.0).toFloat()) * height)
                            path.lineTo(x, height)
                        } else {
                            path.lineTo(x, (decay.coerceIn(0.0, 1.0).toFloat()) * height)
                            path.lineTo(x, 0f)
                        }
                        lastTime = maintTime
                    }
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
                    drawLine(Color.Red, start = Offset(width, 0f), end = Offset(width, height), strokeWidth = 3f)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                Text(text = dateFormat.format(Date(buildDate * 1000)), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopStart))
                Text(text = stringResource(R.string.now_label), style = MaterialTheme.typography.labelSmall, color = Color.Red, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

enum class AppDestinations(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.tab_mines, Icons.Default.Home),
    FACTORIES(R.string.tab_factories, Icons.Default.Build),
    COSTS(R.string.tab_costs, Icons.Default.ShoppingCart),
    HQ(R.string.tab_hq, Icons.Default.LocationOn),
    PROFILE(R.string.tab_profile, Icons.Default.AccountBox),
}
