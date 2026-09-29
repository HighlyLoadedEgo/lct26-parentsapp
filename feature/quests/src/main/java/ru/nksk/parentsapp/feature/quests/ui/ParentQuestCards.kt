package ru.nksk.parentsapp.feature.quests.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import ru.nksk.parentsapp.feature.quests.R

@Composable
fun ParentQuestCards(onOpen: (ParentQuest) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.parents_real_life_quests), style = MaterialTheme.typography.titleMedium)
        ParentQuest.entries.forEach { quest ->
            Card(onClick = dropUnlessResumed { onOpen(quest) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(quest.title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(quest.summary), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.parents_quest_meta, quest.steps.size), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
