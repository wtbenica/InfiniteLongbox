package dev.benica.infinite_longbox.ui.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.R

/** One list on the Collections page: what it is called, and how many issues are in it. */
data class CollectionCard(val id: Int, val name: String, val count: Int)

/** The Collections page: a card for each list the user keeps. */
@Composable
fun CollectionsScreen(cards: List<CollectionCard>, onOpen: (Int) -> Unit) {
    val margin = dimensionResource(R.dimen.margin_wide)
    Column(Modifier.fillMaxSize().padding(margin), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.forEach { card ->
            Card(
                Modifier.fillMaxWidth().clickable { onOpen(card.id) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.radius_list_item_issue)),
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.surface_raised)),
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(card.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.collections_issue_count, card.count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.text_secondary),
                    )
                }
            }
        }
    }
}
