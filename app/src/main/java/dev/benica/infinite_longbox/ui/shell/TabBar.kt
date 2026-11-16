package dev.benica.infinite_longbox.ui.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.nav.Tab

/** The height of the tab bar's row, without the system bar under it. */
val TAB_BAR_HEIGHT = 56.dp

/** The bar at the bottom: the tabs, and Scan, which opens the scanner instead of a tab. */
@Composable
fun TabBar(selected: Tab, onTab: (Tab) -> Unit, onScan: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, color = colorResource(R.color.surface_raised), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(TAB_BAR_HEIGHT)) {
            TabItem(R.drawable.ic_tab_browse, R.string.tab_browse, selected == Tab.BROWSE, { onTab(Tab.BROWSE) }, Modifier.weight(1f))
            TabItem(R.drawable.ic_tab_releases, R.string.tab_releases, selected == Tab.RELEASES, { onTab(Tab.RELEASES) }, Modifier.weight(1f))
            TabItem(R.drawable.ic_tab_collections, R.string.tab_collections, selected == Tab.COLLECTIONS, { onTab(Tab.COLLECTIONS) }, Modifier.weight(1f))
            TabItem(R.drawable.ic_barcode_scan, R.string.tab_scan, false, onScan, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(@DrawableRes icon: Int, @StringRes label: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val color = if (selected) colorResource(R.color.accent) else colorResource(R.color.text_secondary)
    Column(modifier.clickable(onClick = onClick).padding(vertical = 6.dp), androidx.compose.foundation.layout.Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(painterResource(icon), null, Modifier.size(24.dp), tint = color)
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = color)
    }
}


