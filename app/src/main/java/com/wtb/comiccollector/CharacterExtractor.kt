package com.wtb.comiccollector

import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.*
import java.util.regex.Pattern

class CharacterExtractor(val database: IssueDatabase) {
    suspend fun extractCharacters(stories: List<Item<GcdStory, Story>>?) {
        stories?.forEach { gcdStory ->
            val story = gcdStory.fields

            val characters = story.characters.split("; ")

            characters.forEach { character ->
                CoroutineScope(Dispatchers.Default).launch {
                    makeCharacterCredit(character, gcdStory.pk)
                }
            }
        }
    }

    private suspend fun makeCharacterCredit(characterName: String, pk: Int) {

        var info: String? = null
        val infoRegex = Pattern.compile("\\((.*?)\\)")
        val infoMatcher = infoRegex.matcher(characterName)

        if (infoMatcher.find()) {
            info = infoMatcher.group(1)
        }

        val name = characterName.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")

        val character = CoroutineScope(Dispatchers.IO).async {
            database.characterDao().getCharacterByInfo(name)
        }

        character.await().let { chars ->
            if (chars != null && chars.isNotEmpty()) {
                database.appearanceDao().upsertSus(
                    listOf(
                        Appearance(
                            storyId = pk,
                            characterId = chars[0].characterId,
                            details = info
                        )
                    )
                )
            } else {
                withContext(Dispatchers.Default) {
                    database.characterDao().upsertSus(
                        listOf(
                            Character(
                                name = name,
                            )
                        )
                    )
                }.let {
                    withContext(Dispatchers.Default) {
                        database.characterDao().getCharacterByInfo(name)
                    }.let {
                        if (it != null && it.isNotEmpty()) {
                            database.appearanceDao().upsertSus(
                                listOf(
                                    Appearance(
                                        storyId = pk,
                                        characterId = it[0].characterId,
                                        details = info
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleRest(subList: List<String>) {
        TODO("Not yet implemented")
    }

    private fun handleFirst(s: String) {
        TODO("Not yet implemented")
    }
}