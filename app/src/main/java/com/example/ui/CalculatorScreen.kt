package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.CalculatorEngine
import com.example.data.Calculation
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val display by viewModel.displayState.collectAsState()
    val preview by viewModel.previewState.collectAsState()
    val useRadians by viewModel.useRadians.collectAsState()
    val memory by viewModel.memoryValue.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val history by viewModel.historyList.collectAsState()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var isScientificExpanded by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Collect Toast Messages from ViewModel
    LaunchedEffect(viewModel.toastMessage) {
        viewModel.toastMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Responsive configuration
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header (App Title & Theme Switcher)
                HeaderSection(
                    isDarkMode = isDarkMode,
                    onThemeToggle = { viewModel.toggleDarkMode() },
                    onShowHistory = { showHistoryDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Calculator Display Card
                DisplayCard(
                    display = display,
                    preview = preview,
                    modifier = Modifier.weight(1.2f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status row indicators (Memory state, RAD/DEG state)
                StatusBarIndicatorRow(
                    useRadians = useRadians,
                    memoryValue = memory,
                    isScientific = isScientificExpanded,
                    onAngleToggle = { viewModel.toggleAngleMode() },
                    onSciToggle = { isScientificExpanded = !isScientificExpanded }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Response Grid
                Box(
                    modifier = Modifier
                        .weight(3.5f)
                        .fillMaxWidth()
                ) {
                    if (isLandscape || isTablet) {
                        // Tablet / Landscape: Side-by-Side canonical layout
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Scientific Pad (Left side)
                            ScientificKeypad(
                                onAction = { viewModel.onInput(it) },
                                onMemory = { viewModel.onMemoryAction(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            // Numeric Pad (Right side)
                            BasicKeypad(
                                onAction = { viewModel.onInput(it) },
                                onClear = { viewModel.onClear() },
                                onDelete = { viewModel.onDelete() },
                                onEvaluate = { viewModel.onEvaluate() },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        // Portrait Mobile: Top Scientific, Bottom Basic
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AnimatedVisibility(
                                visible = isScientificExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                                modifier = Modifier.weight(1f)
                            ) {
                                ScientificKeypad(
                                    onAction = { viewModel.onInput(it) },
                                    onMemory = { viewModel.onMemoryAction(it) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            BasicKeypad(
                                onAction = { viewModel.onInput(it) },
                                onClear = { viewModel.onClear() },
                                onDelete = { viewModel.onDelete() },
                                onEvaluate = { viewModel.onEvaluate() },
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
            }

            // Calculations History Dialog Bottom Sheet Pattern
            if (showHistoryDialog) {
                HistoryBottomSheet(
                    history = history,
                    onSelect = { expr, res ->
                        viewModel.onHistoryItemSelect(expr, res)
                        showHistoryDialog = false
                    },
                    onDelete = { viewModel.onDeleteHistoryItem(it) },
                    onClearAll = { viewModel.onClearHistory() },
                    onDismiss = { showHistoryDialog = false }
                )
            }
        }
    }
}

@Composable
fun HeaderSection(
    isDarkMode: Boolean?,
    onThemeToggle: () -> Unit,
    onShowHistory: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Scientific",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )
            Text(
                text = "Calculator",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onShowHistory,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "History Log",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onThemeToggle,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("theme_toggle_button")
            ) {
                Text(
                    text = when (isDarkMode) {
                        null -> "🌓"
                        true -> "🌙"
                        false -> "☀️"
                    },
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun DisplayCard(
    display: String,
    preview: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Keep scrolling to the end when display text changes
    LaunchedEffect(display) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Previous computation equation or real-time preview display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Primary input display with blinker simulation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = display.ifEmpty { "0" },
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (display.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                )

                // Blinking cursor if display is active
                BlinkingCursor()
            }
        }
    }
}

@Composable
fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor_blink")
    val alpha by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_opacity"
    )

    Box(
        modifier = Modifier
            .width(2.dp)
            .height(36.dp)
            .padding(start = 2.dp)
            .graphicsLayer(alpha = alpha)
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun StatusBarIndicatorRow(
    useRadians: Boolean,
    memoryValue: Double,
    isScientific: Boolean,
    onAngleToggle: () -> Unit,
    onSciToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DEG / RAD Selector Chip
            SuggestionChip(
                onClick = onAngleToggle,
                label = {
                    Text(
                        text = if (useRadians) "RAD" else "DEG",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("angle_mode_chip")
            )

            // Memory Indicator Chip if active
            if (memoryValue != 0.0) {
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            text = "M: ${CalculatorEngine.formatResult(memoryValue)}",
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        // Toggle Expand Scientific Drawer
        InputChip(
            selected = isScientific,
            onClick = onSciToggle,
            label = {
                Text(
                    text = if (isScientific) "Simple" else "Scientific",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            trailingIcon = {
                Text(text = if (isScientific) "▲" else "▼")
            },
            modifier = Modifier.testTag("sci_toggle_chip")
        )
    }
}

@Composable
fun ScientificKeypad(
    onAction: (String) -> Unit,
    onMemory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("MC", "MR", "M+", "M-", "^"),
        listOf("sin(", "cos(", "tan(", "log(", "ln("),
        listOf("√(", "π", "e", "(", ")")
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in keys) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (key in row) {
                    val isMemory = key.startsWith("M") && key != "sin(" && key != "cos(" && key != "tan("
                    CalculatorButton(
                        text = key.replace("(", ""),
                        onClick = {
                            if (isMemory) onMemory(key) else onAction(key)
                        },
                        containerColor = if (isMemory) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        textColor = if (isMemory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("scientific_key_$key")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicKeypad(
    onAction: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onEvaluate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val operatorColor = MaterialTheme.colorScheme.secondary
    val numColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: AC, %, Delete, Division
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // AC supports combinedClickable for long-click delete to be extremely satisfying
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .combinedClickable(
                        onClick = onClear,
                        onLongClick = onClear
                    )
                    .testTag("key_clear_ac")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "AC",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            CalculatorButton(
                text = "%",
                onClick = { onAction("%") },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            CalculatorButton(
                text = "⌫",
                onClick = onDelete,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("key_backspace")
            )

            CalculatorButton(
                text = "÷",
                onClick = { onAction("÷") },
                containerColor = operatorColor,
                textColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 2: 7, 8, 9, Multi
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("7", "8", "9").forEach { num ->
                CalculatorButton(
                    text = num,
                    onClick = { onAction(num) },
                    containerColor = numColor,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            CalculatorButton(
                text = "×",
                onClick = { onAction("×") },
                containerColor = operatorColor,
                textColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 3: 4, 5, 6, Subtract
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("4", "5", "6").forEach { num ->
                CalculatorButton(
                    text = num,
                    onClick = { onAction(num) },
                    containerColor = numColor,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            CalculatorButton(
                text = "−",
                onClick = { onAction("−") },
                containerColor = operatorColor,
                textColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 4: 1, 2, 3, Plus
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1", "2", "3").forEach { num ->
                CalculatorButton(
                    text = num,
                    onClick = { onAction(num) },
                    containerColor = numColor,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            CalculatorButton(
                text = "+",
                onClick = { onAction("+") },
                containerColor = operatorColor,
                textColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 5: 0, Dot, Equal
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorButton(
                text = "0",
                onClick = { onAction("0") },
                containerColor = numColor,
                textColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            )

            CalculatorButton(
                text = ".",
                onClick = { onAction(".") },
                containerColor = numColor,
                textColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            CalculatorButton(
                text = "=",
                onClick = onEvaluate,
                containerColor = primaryColor,
                textColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("key_evaluate")
            )
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile elastic spring animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "btn_spring"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (text.length > 2) 16.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun HistoryBottomSheet(
    history: List<Calculation>,
    onSelect: (String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calculation History",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                if (history.isNotEmpty()) {
                    IconButton(onClick = onClearAll) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Wipe history",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📚",
                            fontSize = 36.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "No history available.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history, key = { it.id }) { item ->
                        HistoryItemView(
                            item = item,
                            onClick = { onSelect(item.expression, item.result) },
                            onDelete = { onDelete(item.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(26.dp)
    )
}

@Composable
fun HistoryItemView(
    item: Calculation,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.expression,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    ),
                    maxLines = 2
                )
                Text(
                    text = "= ${item.result}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_history_item_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Item",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
