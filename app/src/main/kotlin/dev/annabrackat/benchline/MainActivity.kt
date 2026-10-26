package dev.annabrackat.benchline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.annabrackat.benchline.ui.BenchlineApp
import dev.annabrackat.benchline.ui.BenchlineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BenchlineTheme {
                BenchlineApp()
            }
        }
    }
}
