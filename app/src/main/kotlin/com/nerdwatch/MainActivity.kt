package com.nerdwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * The single Activity host. All navigation between the face, the timer pages
 * and the sub-screens happens in Compose, not with more Activities.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NerdWatchApp() }
    }
}
