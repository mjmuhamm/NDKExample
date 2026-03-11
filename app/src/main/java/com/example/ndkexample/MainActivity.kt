package com.example.ndkexample

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.example.ndkexample.data.NativeProcessor
import com.example.ndkexample.domain.BenckmarkStats
import com.example.ndkexample.ui.theme.NDKExampleTheme
import kotlinx.coroutines.NonCancellable.key

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NDKExampleTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ImageProcessorScreen()
                }
            }
        }
    }

}


@Composable
fun ImageProcessorScreen() {
    val context = LocalContext.current

    val masterBitmap = remember {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeResource(context.resources, R.drawable.pexels_inspiredimages_132474, this)
            inSampleSize = calculateInSampleSize(this, 1024, 1024)
            inJustDecodeBounds = false
        }
        BitmapFactory.decodeResource(context.resources, R.drawable.pexels_inspiredimages_132474, options) ?: createBitmap(100,100)
    }
    var workingBitmap by remember { mutableStateOf(masterBitmap.copy(Bitmap.Config.ARGB_8888, true)) }

    var currentStats by remember { mutableStateOf(BenckmarkStats())}
    var updateTick by remember { mutableIntStateOf(0) }

    var useWarmUp by remember { mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("NDK vs Kotlin Performance", style = MaterialTheme.typography.headlineMedium)
        key(updateTick) {
            Image(
                bitmap = workingBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(300.dp)
            )
        }

        if (currentStats.mode.isNotEmpty()) {
            BenchmarkTable(stats = currentStats)
        }

        Row() {
            Switch(checked = useWarmUp, onCheckedChange = {useWarmUp = it})
            Text("Enable 10x Iteration (Warm-up", modifier = Modifier.padding(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = {
                // measure first run separately
                val start = System.currentTimeMillis()
                NativeProcessor.convertToGray(workingBitmap)
                val firstNative = System.currentTimeMillis() - start

                var warmAvg = 0L

                if (useWarmUp) {
                    val warmStart = System.currentTimeMillis()
                    repeat(9) {
                        NativeProcessor.convertToGray(workingBitmap)
                    }
                    warmAvg = (System.currentTimeMillis() - warmStart) / 9
                }

                currentStats = BenckmarkStats("Native", firstNative, warmAvg, false)
            }) {
                Text("Run Native")
            }

            Button(onClick = {
                // measure first run separately
                val start = System.currentTimeMillis()
                NativeProcessor.convertToGrayKotlin(workingBitmap)
                val firstKotlin = System.currentTimeMillis() - start

                var warmAvg = 0L

                if (useWarmUp) {
                    val warmStart = System.currentTimeMillis()
                    repeat(9) {
                        NativeProcessor.convertToGrayKotlin(workingBitmap)
                    }
                    warmAvg = (System.currentTimeMillis() - warmStart) / 9
                }

                currentStats = BenckmarkStats("Kotlin", firstKotlin, warmAvg, false)
            }) {
                Text("Run Kotlin")
            }
        }

        Button(onClick = {
            workingBitmap.recycle()
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(this, 1024, 1024)
                inMutable = true
            }
            workingBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pexels_inspiredimages_132474, options) ?: createBitmap(100 , 100)

            //clear benchmark stats and force-trigger garbage collection
            //cannot reset JIT performance optimization
            //without restarting app

            currentStats = BenckmarkStats()
            updateTick++
            System.gc()
        }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Deep Reset (Clear Cache")
        }
    }

}

@Composable
fun BenchmarkTable(stats: BenckmarkStats) {
    Column(modifier =  Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(8.dp)).background(
        MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {

        Text(
            text = "Performance Profile: ${stats.mode}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Cold Start (1st run):", style = MaterialTheme.typography.bodyMedium)
            Text("${stats.firstRun}ms", fontWeight = FontWeight.Bold, color = Color.Red)
        }
        if (stats.warmAvg > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Warm Average (JIT):", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.warmAvg}ms", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
        }

        val delta = if (stats.warmAvg > 0) stats.firstRun - stats.warmAvg else 0
        if (delta > 0) {
            Text(text = "JIT Optimization Gain: ${delta}ms faster", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp), fontStyle = FontStyle.Italic)

        }
    }

}

private fun calculateInSampleSize(options: BitmapFactory.Options,
                                  reqWidth: Int,
                                  reqHeight: Int) : Int {
    val (height: Int, width: Int)  = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize

}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NDKExampleTheme {
        Greeting("Android")
    }
}

