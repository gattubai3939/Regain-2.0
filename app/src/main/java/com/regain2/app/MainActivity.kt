package com.regain2.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.regain2.app.ui.MainScreen
import com.regain2.app.ui.RegainTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RegainTheme { MainScreen(vm) } }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }
}
