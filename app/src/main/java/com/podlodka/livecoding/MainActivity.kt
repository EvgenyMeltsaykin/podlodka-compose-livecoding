package com.podlodka.livecoding

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.podlodka.livecoding.ui.theme.PodlodkaLiveCodingTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class Code(
    var code: String,
    var progress: Float,
    var isActive: Boolean,
) {
    var formattedCode = code.take(6)
}

class MainActivity : ComponentActivity() {
    private var intervalJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PodlodkaLiveCodingTheme {
                var codes by remember { mutableStateOf(listOf<Code>()) }

                LaunchedEffect(codes) {
                    intervalJob?.cancel()
                    intervalJob = lifecycleScope.launch {
                        while (true) {
                            delay(100)
                            codes = codes.map { code ->
                                val newProgress = code.progress + 0.02f
                                Code(
                                    code = if (newProgress > 1) UUID.randomUUID().toString() else code.code,
                                    progress = if (newProgress > 1) 0f else newProgress,
                                    isActive = code.isActive
                                )
                            }
                        }
                    }
                }

                Log.d("LiveCoding", "Current codes: $codes")

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    BoxWithConstraints(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {

                        var topBarHeight by remember { mutableStateOf(0) }
                        val density = LocalDensity.current
                        CodeTopBar(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(this.maxWidth)
                                .onSizeChanged {
                                    topBarHeight = it.height
                                }
                        )

                        Column(
                            modifier = Modifier
                                .padding(top = with(density) { topBarHeight.toDp() })
                                .align(Alignment.TopCenter)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            codes.forEach { code ->
                                val animateProgress = animateFloatAsState(code.progress)
                                val animationProgress = animateProgress.value
                                CodeContent(
                                    code = code,
                                    animationProgress = animationProgress,
                                    onClick = {
                                        code.isActive = !code.isActive
                                    },
                                    onLongClick = {
                                        codes = codes.filter { it.code != code.code }
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.Blue.copy(alpha = 0.5f), CircleShape),
                            onClick = {
                                codes = codes + Code(code = UUID.randomUUID().toString().take(6), progress = 0f, isActive = true)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CodeContent(
    code: Code,
    animationProgress: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .clip(RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .then(
                if (code.isActive) {
                    Modifier.background(
                        Color.Red.copy(alpha = animationProgress),
                        RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val text = if (code.isActive) {
            "Active"
        } else {
            "Inactive"
        }
        Column {
            Text(
                text = code.formattedCode.uppercase(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        val progressAlpha = if (code.isActive) 1f else 0f
        CircularProgressIndicator(
            modifier = Modifier
                .size(16.dp)
                .alpha(progressAlpha),
            strokeWidth = 2.dp,
            strokeCap = StrokeCap.Round,
            progress = { animationProgress }
        )
    }
}

@Composable
private fun CodeTopBar(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(16.dp),
        text = "Список кодов",
        style = MaterialTheme.typography.headlineMedium
    )
}