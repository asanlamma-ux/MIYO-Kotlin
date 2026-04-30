package com.miyu.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.miyu.reader.ui.MIYUApp
import com.miyu.reader.ui.theme.MIYUTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("miyu_engine")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MIYUTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MIYUApp()
                }
            }
        }
    }
}