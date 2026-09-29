package ru.nksk.parentsapp.app.navigation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.nksk.parentsapp.R
import ru.nksk.parentsapp.core.report.rewards.ParentRewardCaps

/** App-owned bundled artwork; the quest feature receives only a composable slot. */
@Composable
internal fun ParentCapImage(itemId: String, modifier: Modifier) {
    val resource = when (ParentRewardCaps.forItem(itemId)?.lookId) {
        "CAP_MOSCOW_BLUE" -> R.drawable.gear_cap_moscow_blue
        "CAP_MOSCOW_EMERALD" -> R.drawable.gear_cap_moscow_emerald
        "CAP_MOSCOW_BURGUNDY" -> R.drawable.gear_cap_moscow_burgundy
        "CAP_LCT2026_BLUE" -> R.drawable.gear_cap_lct2026_blue
        "CAP_LCT2026_EMERALD" -> R.drawable.gear_cap_lct2026_emerald
        "CAP_LCT2026_BURGUNDY" -> R.drawable.gear_cap_lct2026_burgundy
        else -> return
    }
    val resources = LocalContext.current.resources
    val bitmap by produceState<ImageBitmap?>(null, resource, resources) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeResource(resources, resource)?.asImageBitmap()
        }
    }
    val image = bitmap
    if (image == null) Box(modifier)
    else Image(image, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
}
