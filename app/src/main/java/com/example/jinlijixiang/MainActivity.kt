package com.example.jinlijixiang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.jinlijixiang.ui.screen.CalculatorScreen
import com.example.jinlijixiang.ui.theme.JinlijixiangTheme

/**
 * 金力吉祥报价计算器 - 主Activity
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JinlijixiangTheme {
                CalculatorScreen()
            }
        }
    }
}
