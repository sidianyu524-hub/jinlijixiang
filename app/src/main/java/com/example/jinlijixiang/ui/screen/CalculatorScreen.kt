package com.example.jinlijixiang.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jinlijixiang.data.UpdateInfo
import com.example.jinlijixiang.ui.components.DownloadProgressDialog
import com.example.jinlijixiang.ui.components.UpdateDialog
import com.example.jinlijixiang.utils.AppUpdateManager
import kotlinx.coroutines.launch
import android.widget.Toast

/**
 * 深色/浅色模式颜色配置
 */
object AppColors {
    // 浅色模式
    val lightBackground = Color(0xFFF5F5F5)
    val lightCardBackground = Color.White
    val lightTextPrimary = Color(0xFF212121)
    val lightTextSecondary = Color(0xFF666666)
    val lightBorder = Color(0xFFBDBDBD)
    val lightPriceBackground = Color(0xFFF0F0F0)

    // 深色模式 - 高对比度
    val darkBackground = Color(0xFF121212)
    val darkCardBackground = Color(0xFF1E1E1E)
    val darkTextPrimary = Color(0xFFFFFFFF)
    val darkTextSecondary = Color(0xFFB0B0B0)
    val darkBorder = Color(0xFF555555)
    val darkPriceBackground = Color(0xFF2A2A2A)

    // 主题色（两种模式通用）
    val primary = Color(0xFF1976D2)
    val primaryLight = Color(0xFF42A5F5)
    val accent = Color(0xFFFF9800)
}

/**
 * 箱体价格计算器主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isDarkTheme = isSystemInDarkTheme()

    // 根据主题获取颜色
    val backgroundColor = if (isDarkTheme) AppColors.darkBackground else AppColors.lightBackground
    val cardBackground = if (isDarkTheme) AppColors.darkCardBackground else AppColors.lightCardBackground
    val textPrimary = if (isDarkTheme) AppColors.darkTextPrimary else AppColors.lightTextPrimary
    val textSecondary = if (isDarkTheme) AppColors.darkTextSecondary else AppColors.lightTextSecondary
    val borderColor = if (isDarkTheme) AppColors.darkBorder else AppColors.lightBorder
    val priceBackground = if (isDarkTheme) AppColors.darkPriceBackground else AppColors.lightPriceBackground

    // 输入字段
    var boxLength by remember { mutableStateOf("500") }
    var boxWidth by remember { mutableStateOf("400") }
    var boxHeight by remember { mutableStateOf("300") }
    var boxUnitPrice by remember { mutableStateOf("300") }

    // 上盖海绵
    var hasTopSponge by remember { mutableStateOf(false) }
    var topSpongeLength by remember { mutableStateOf("") }
    var topSpongeWidth by remember { mutableStateOf("") }
    var topSpongeHeight by remember { mutableStateOf("") }
    var topSpongeUnitPrice by remember { mutableStateOf("3000") }

    // 下拖海绵
    var hasBottomSponge by remember { mutableStateOf(false) }
    var bottomSpongeLength by remember { mutableStateOf("") }
    var bottomSpongeWidth by remember { mutableStateOf("") }
    var bottomSpongeHeight by remember { mutableStateOf("") }
    var bottomSpongeUnitPrice by remember { mutableStateOf("3000") }

    // 计算结果
    var boxPrice by remember { mutableStateOf(0.0) }
    var topSpongePrice by remember { mutableStateOf(0.0) }
    var bottomSpongePrice by remember { mutableStateOf(0.0) }
    var totalPrice by remember { mutableStateOf(0.0) }

    // 更新相关状态
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    val currentVersion = remember { AppUpdateManager.getCurrentVersionName(context) }

    // 计算箱体价格
    fun calculateBoxPrice(): Double {
        val length = boxLength.toDoubleOrNull() ?: 0.0
        val width = boxWidth.toDoubleOrNull() ?: 0.0
        val height = boxHeight.toDoubleOrNull() ?: 0.0
        val unitPrice = boxUnitPrice.toDoubleOrNull() ?: 0.0

        if (length <= 0 || width <= 0 || height <= 0) return 0.0

        // 表面积 = 2 × (长×宽 + 长×高 + 宽×高)，单位转换为平方米
        val surfaceArea = 2 * ((length * width) + (length * height) + (width * height)) / 1000000
        return surfaceArea * unitPrice
    }

    // 计算海绵价格
    fun calculateSpongePrice(length: String, width: String, height: String, unitPrice: String): Double {
        val l = length.toDoubleOrNull() ?: 0.0
        val w = width.toDoubleOrNull() ?: 0.0
        val h = height.toDoubleOrNull() ?: 0.0
        val price = unitPrice.toDoubleOrNull() ?: 0.0

        if (l <= 0 || w <= 0 || h <= 0) return 0.0

        // 体积 = 长 × 宽 × 高，单位转换为立方米
        val volume = (l * w * h) / 1000000000
        return volume * price
    }

    // 计算总价
    fun calculate() {
        boxPrice = calculateBoxPrice()
        topSpongePrice = if (hasTopSponge) {
            calculateSpongePrice(topSpongeLength, topSpongeWidth, topSpongeHeight, topSpongeUnitPrice)
        } else 0.0
        bottomSpongePrice = if (hasBottomSponge) {
            calculateSpongePrice(bottomSpongeLength, bottomSpongeWidth, bottomSpongeHeight, bottomSpongeUnitPrice)
        } else 0.0
        totalPrice = boxPrice + topSpongePrice + bottomSpongePrice
    }

    // 检查更新
    fun checkUpdate() {
        scope.launch {
            isCheckingUpdate = true
            val info = AppUpdateManager.checkUpdate(context)
            isCheckingUpdate = false
            if (info != null) {
                updateInfo = info
                showUpdateDialog = true
            } else {
                Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 初始计算
    LaunchedEffect(Unit) {
        calculate()
    }

    // 更新对话框
    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            currentVersion = currentVersion,
            onUpdate = {
                showUpdateDialog = false
                showDownloadDialog = true
                downloadProgress = 0

                AppUpdateManager.downloadAndInstall(
                    context = context,
                    updateInfo = updateInfo!!,
                    onProgress = { progress ->
                        downloadProgress = progress
                    },
                    onComplete = {
                        showDownloadDialog = false
                        Toast.makeText(context, "下载完成，正在安装...", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        showDownloadDialog = false
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onDismiss = {
                showUpdateDialog = false
            }
        )
    }

    // 下载进度对话框
    if (showDownloadDialog) {
        DownloadProgressDialog(
            progress = downloadProgress,
            onCancel = {
                AppUpdateManager.cancelDownload(context)
                showDownloadDialog = false
                Toast.makeText(context, "已取消下载", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "金利机箱报价计算器",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "v$currentVersion",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 箱体尺寸卡片
            SectionCard(
                title = "箱体尺寸",
                cardBackground = cardBackground,
                textColor = textPrimary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InputField(
                        value = boxLength,
                        onValueChange = { boxLength = it; calculate() },
                        label = "长度",
                        unit = "mm",
                        modifier = Modifier.weight(1f),
                        textColor = textPrimary,
                        borderColor = borderColor
                    )
                    InputField(
                        value = boxWidth,
                        onValueChange = { boxWidth = it; calculate() },
                        label = "宽度",
                        unit = "mm",
                        modifier = Modifier.weight(1f),
                        textColor = textPrimary,
                        borderColor = borderColor
                    )
                    InputField(
                        value = boxHeight,
                        onValueChange = { boxHeight = it; calculate() },
                        label = "高度",
                        unit = "mm",
                        modifier = Modifier.weight(1f),
                        textColor = textPrimary,
                        borderColor = borderColor
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                InputField(
                    value = boxUnitPrice,
                    onValueChange = { boxUnitPrice = it; calculate() },
                    label = "箱体单价",
                    unit = "元/m²",
                    modifier = Modifier.fillMaxWidth(),
                    textColor = textPrimary,
                    borderColor = borderColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                PriceDisplay(
                    label = "箱体价格",
                    price = boxPrice,
                    backgroundColor = priceBackground,
                    textColor = textSecondary
                )
            }

            // 上盖海绵卡片
            SectionCard(
                title = "上盖海绵",
                hasCheckbox = true,
                checked = hasTopSponge,
                onCheckedChange = { hasTopSponge = it; calculate() },
                cardBackground = cardBackground,
                textColor = textPrimary
            ) {
                if (hasTopSponge) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InputField(
                            value = topSpongeLength,
                            onValueChange = { topSpongeLength = it; calculate() },
                            label = "长度",
                            unit = "mm",
                            modifier = Modifier.weight(1f),
                            textColor = textPrimary,
                            borderColor = borderColor
                        )
                        InputField(
                            value = topSpongeWidth,
                            onValueChange = { topSpongeWidth = it; calculate() },
                            label = "宽度",
                            unit = "mm",
                            modifier = Modifier.weight(1f),
                            textColor = textPrimary,
                            borderColor = borderColor
                        )
                        InputField(
                            value = topSpongeHeight,
                            onValueChange = { topSpongeHeight = it; calculate() },
                            label = "高度",
                            unit = "mm",
                            modifier = Modifier.weight(1f),
                            textColor = textPrimary,
                            borderColor = borderColor
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    InputField(
                        value = topSpongeUnitPrice,
                        onValueChange = { topSpongeUnitPrice = it; calculate() },
                        label = "海绵单价",
                        unit = "元/m³",
                        modifier = Modifier.fillMaxWidth(),
                        textColor = textPrimary,
                        borderColor = borderColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PriceDisplay(
                        label = "上盖海绵价格",
                        price = topSpongePrice,
                        backgroundColor = priceBackground,
                        textColor = textSecondary
                    )
                }
            }

            // 下拖海绵卡片
            SectionCard(
                title = "下拖海绵",
                hasCheckbox = true,
                checked = hasBottomSponge,
                onCheckedChange = { hasBottomSponge = it; calculate() },
                cardBackground = cardBackground,
                textColor = textPrimary
            ) {
                if (hasBottomSponge) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InputField(
                            value = bottomSpongeLength,
                            onValueChange = { bottomSpongeLength = it; calculate() },
                            label = "长度",
                            unit = "mm",
                            modifier = Modifier.weight(1f),
                            textColor = textPrimary,
                            borderColor = borderColor
                        )
                        InputField(
                            value = bottomSpongeWidth,
                            onValueChange = { bottomSpongeWidth = it; calculate() },
                            label = "宽度",
                            unit = "mm",
                            modifier = Modifier.weight(1f),
                            textColor = textPrimary,
                            borderColor = borderColor
                        )
                        InputField(
                            value = bottomSpongeHeight,
                            onValueChange = { bottomSpongeHeight = it; calculate() },
                            label = "高度",
                            unit = "mm",
                            modifier = Modifier.weight(1f),
                            textColor = textPrimary,
                            borderColor = borderColor
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    InputField(
                        value = bottomSpongeUnitPrice,
                        onValueChange = { bottomSpongeUnitPrice = it; calculate() },
                        label = "海绵单价",
                        unit = "元/m³",
                        modifier = Modifier.fillMaxWidth(),
                        textColor = textPrimary,
                        borderColor = borderColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PriceDisplay(
                        label = "下拖海绵价格",
                        price = bottomSpongePrice,
                        backgroundColor = priceBackground,
                        textColor = textSecondary
                    )
                }
            }

            // 总价卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(AppColors.primary, AppColors.primaryLight)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "总报价",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "¥ ${String.format("%.2f", totalPrice)}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 检查更新按钮
            Button(
                onClick = { checkUpdate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.accent
                ),
                enabled = !isCheckingUpdate
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("检查中...", fontSize = 16.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("检查更新", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 区块卡片
 */
@Composable
fun SectionCard(
    title: String,
    hasCheckbox: Boolean = false,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    cardBackground: Color = Color.White,
    textColor: Color = Color(0xFF333333),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasCheckbox) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = AppColors.primary,
                            uncheckedColor = textColor.copy(alpha = 0.6f),
                            checkmarkColor = Color.White
                        )
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = if (hasCheckbox) Modifier else Modifier.padding(bottom = 12.dp)
                )
            }
            if (!hasCheckbox || checked) {
                if (hasCheckbox) Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

/**
 * 输入框
 */
@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF333333),
    borderColor: Color = Color(0xFFBDBDBD)
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // 只允许数字和小数点
            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(newValue)
            }
        },
        label = { Text(label, fontSize = 12.sp, color = textColor.copy(alpha = 0.7f)) },
        suffix = { Text(unit, color = textColor.copy(alpha = 0.5f), fontSize = 12.sp) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedBorderColor = AppColors.primary,
            unfocusedBorderColor = borderColor,
            cursorColor = AppColors.primary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

/**
 * 价格显示
 */
@Composable
fun PriceDisplay(
    label: String,
    price: Double,
    backgroundColor: Color = Color(0xFFF5F5F5),
    textColor: Color = Color(0xFF666666)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = textColor
        )
        Text(
            text = "¥ ${String.format("%.2f", price)}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.primary
        )
    }
}
