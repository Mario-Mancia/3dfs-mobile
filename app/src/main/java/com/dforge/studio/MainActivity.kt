package com.dforge.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dforge.studio.ui.navigation.DForgeApp
import com.dforge.studio.ui.theme.DForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DForgeTheme {
                DForgeApp()
            }
        }
    }
}
