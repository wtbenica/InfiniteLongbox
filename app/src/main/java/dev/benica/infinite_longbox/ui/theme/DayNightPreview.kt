package dev.benica.infinite_longbox.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** A preview in the day theme and again in the night theme, from one function. */
@Preview(name = "Day", showBackground = true)
@Preview(name = "Night", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class DayNightPreview

/** [DayNightPreview] at a phone's width. */
@Preview(name = "Day", showBackground = true, widthDp = 360)
@Preview(name = "Night", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class DayNightPreview360

/** [DayNightPreview] at a phone's width, for a whole screen. */
@Preview(name = "Day", showBackground = true, widthDp = 360, heightDp = 900)
@Preview(name = "Night", showBackground = true, widthDp = 360, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class DayNightPreview360Tall

/** [DayNightPreview] at the width of a grid cell. */
@Preview(name = "Day", showBackground = true, widthDp = 200)
@Preview(name = "Night", showBackground = true, widthDp = 200, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class DayNightPreview200
