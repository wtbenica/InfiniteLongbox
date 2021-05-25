package com.wtb.comiccollector.repository

class ExtractCredit {
//        suspend fun extractCredits(stories: List<Item<GcdStory, Story>>?) {
//            stories?.forEach { gcdStory ->
//                val story = gcdStory.fields
//
//                checkField(story.script, gcdStory.pk, Role.Companion.Name.SCRIPT.value)
//                checkField(story.pencils, gcdStory.pk, Role.Companion.Name.PENCILS.value)
//                checkField(story.inks, gcdStory.pk, Role.Companion.Name.INKS.value)
//                checkField(story.colors, gcdStory.pk, Role.Companion.Name.COLORS.value)
//                checkField(story.letters, gcdStory.pk, Role.Companion.Name.LETTERS.value)
//                checkField(story.editing, gcdStory.pk, Role.Companion.Name.EDITING.value)
//            }
//        }
//
//        private suspend fun checkField(value: String, storyId: Int, roleId: Int) {
//            if (value != "" && value != "?") {
//                value.split("; ").map { name ->
//                    var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
//                    res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
//                    makeCredit(res, storyId, roleId)
//                }
//            }
//        }
//
//        private suspend fun makeCredit(extracted_name: String, storyId: Int, roleId: Int) {
//
//            val checkLocalCreators = GlobalScope.async {
//                creatorDao.getCreatorByNameSus(extracted_name)
//            }
//
//            checkLocalCreators.await().let { localCreators: List<Creator>? ->
//                if (localCreators == null || localCreators.isEmpty()) {
//                    val nameDetails = GlobalScope.async {
//                        apiService.getNameDetailByName(extracted_name)
//                            .map { it.toRoomModel() }
//                    }
//
//                    nameDetails.await().let { nameDetailItems1: List<NameDetail> ->
//                        if (nameDetailItems1.isNotEmpty()) {
//                            val creatorIds = nameDetailItems1.map { it.creatorId }
//
//                            val creators = GlobalScope.async {
//                                if (creatorIds.isNotEmpty()) {
//                                    apiService.getCreator(creatorIds)
//                                        .map { it.toRoomModel() }
//                                } else {
//                                    null
//                                }
//                            }
//
//                            creators.await()?.let { it: List<Creator> ->
//                                if (it.size > 1) {
//                                    Log.d(
//                                        TAG,
//                                        "Multiple creator matches: $extracted_name ${it.size}"
//                                    )
//                                }
//                                withContext(Dispatchers.Default) {
//                                    creatorDao.upsertSus(it)
//                                }.let {
//                                    withContext(Dispatchers.Default) {
//                                        nameDetailDao.upsertSus(nameDetailItems1)
//                                    }.let {
//                                        withContext(Dispatchers.Default) {
//                                            apiService.getNameDetailsByCreatorIds(creatorIds)
//                                        }
//                                            .let { ndItems: List<Item<GcdNameDetail, NameDetail>> ->
//                                                withContext(Dispatchers.Default) {
//                                                    nameDetailDao.upsertSus(ndItems.map { it.toRoomModel() })
//                                                }.let {
//                                                    creditDao.upsertSus(
//                                                        listOf(
//                                                            Credit(
//                                                                storyId = storyId,
//                                                                nameDetailId = ndItems[0].pk,
//                                                                roleId = roleId
//                                                            )
//                                                        )
//                                                    )
//                                                }
//                                            }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    GlobalScope.launch {
//                        val nameDetail = GlobalScope.async {
//                            localCreators[0].creatorId.let { id ->
//                                nameDetailDao.getNameDetailByCreatorIdSus(id)
//                            }
//                        }
//
//                        nameDetail.await().let {
//                            it?.let {
//                                creditDao.upsertSus(
//                                    listOf(
//                                        Credit(
//                                            storyId = storyId,
//                                            nameDetailId = it.nameDetailId,
//                                            roleId = roleId
//                                        )
//                                    )
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
}