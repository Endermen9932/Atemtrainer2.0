package com.atemtrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.atemtrainer.ui.navigation.AppNavGraph
import com.atemtrainer.ui.theme.AtemtrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtemtrainerTheme {
                AppNavGraph()
            }
        }
    }
}
