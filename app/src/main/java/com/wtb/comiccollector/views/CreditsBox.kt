/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wtb.comiccollector.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FullAppearance
import com.wtb.comiccollector.database.models.FullCredit
import com.wtb.comiccollector.database.models.Story
import com.wtb.comiccollector.fragments.RoleNameTextView
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val STORY_TYPE_COVER = 6

@SuppressLint("ViewConstructor")
@ExperimentalCoroutinesApi
class CreditsBox(context: Context, private var mCallback: CreditsBoxCallback) :
    TableLayout(context), StoryBox.StoryBoxCallback {

    interface CreditsBoxCallback : CharacterLink.CharacterLinkCallback,
        CreatorLink.CreatorLinkCallback

    private var mIssueStories: List<Story> = emptyList()
    private var mVariantStories: List<Story> = emptyList()
    private var mIssueCredits: List<FullCredit> = emptyList()
    private var mVariantCredits: List<FullCredit> = emptyList()
    private var mIssueAppearances: List<FullAppearance> = emptyList()
    private var mVariantAppearances: List<FullAppearance> = emptyList()
    private val integratedStories
        get() = getIntegratedStoryList(mIssueStories, mVariantStories)
    private val expandedStories: MutableSet<Int> = mutableSetOf()

    init {
        orientation = VERTICAL
        layoutParams =
            LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        isStretchAllColumns = true
    }

    fun update(
        issueStories: List<Story>? = null,
        variantStories: List<Story>? = null,
        issueCredits: List<FullCredit>? = null,
        variantCredits: List<FullCredit>? = null,
        issueAppearances: List<FullAppearance>? = null,
        variantAppearances: List<FullAppearance>? = null,
    ) {
        issueStories?.let { this.mIssueStories = it }
        variantStories?.let { this.mVariantStories = it }
        issueCredits?.let { this.mIssueCredits = it }
        variantCredits?.let { this.mVariantCredits = it }
        issueAppearances?.let { this.mIssueAppearances = it }
        variantAppearances?.let { this.mVariantAppearances = it }
        showStories()
    }

    private fun showStories() {
        removeAllViews()
        integratedStories.forEachIndexed { i, story ->
            val appearances = (mIssueAppearances + mVariantAppearances).filter {
                it.appearance.story == story.storyId
            }

            val credits = (mIssueCredits + mVariantCredits).filter {
                it.story.storyId == story.storyId
            }
            val isExpanded = story.storyId in expandedStories
            this.addView(
                StoryBox(
                    context = context,
                    mStory = story,
                    mAppearances = appearances,
                    mCredits = credits,
                    cb = mCallback,
                    isExpanded = isExpanded,
                    sbc = this
                )
            )
        }
    }

    /**
     * Get complete variant stories - returns the full story list for the variant issues
     */
    private fun getIntegratedStoryList(
        original: List<Story>,
        variant: List<Story>,
    ): List<Story> =
        if (STORY_TYPE_COVER in variant.map { it.storyType }) {
            original.mapNotNull {
                if (it.storyType != STORY_TYPE_COVER) {
                    it
                } else {
                    null
                }
            } + variant
        } else {
            original + variant
        }.sortedBy { it.storyType }

    companion object {
        private const val TAG = APP + "CreditsBox"
    }

    override fun setExpanded(storyId: Int, isExpanded: Boolean) {
        Log.d(TAG, "BEFORE: $expandedStories")
        if (isExpanded) {
            expandedStories.add(storyId)
        } else {
            expandedStories.remove(storyId)
        }
        Log.d(TAG, "AFTER: $expandedStories")
    }
}

@SuppressLint("ViewConstructor")
@ExperimentalCoroutinesApi
class StoryBox(
    context: Context,
    private var mStory: Story,
    private var mAppearances: List<FullAppearance>,
    private var mCredits: List<FullCredit>,
    private val cb: CreditsBox.CreditsBoxCallback,
    private var isExpanded: Boolean,
    private val sbc: StoryBoxCallback
) :
    LinearLayout(context, null) {

    private val storyDetailButton: ExpandButton
    private val storyDetailBox: LinearLayout
    private val synopsisBox: LinearLayout
    private val charactersBox: LinearLayout
    private val storyTitleTextView: TextView

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        inflate(context, R.layout.view_story_box, this)

        storyTitleTextView = findViewById(R.id.story_box_title)

        storyDetailBox = findViewById<LinearLayout>(R.id.story_box_details_box).apply {
            visibility =
                if (isExpanded) {
                    VISIBLE
                } else {
                    GONE
                }
        }

        storyDetailButton = findViewById<ExpandButton>(R.id.story_dropdown_button).apply {
            initExpanded(isExpanded)
            setOnClickListener {
                if (storyDetailBox.toggleVisibility()) {
                    Log.d(APP + "StoryBox", "SetExpanded: ${mStory.storyId} TRUE")
                    sbc.setExpanded(mStory.storyId, true)
                } else {
                    Log.d(APP + "StoryBox", "SetExpanded: ${mStory.storyId} FALSE")
                    sbc.setExpanded(mStory.storyId, false)
                }
                (it as ExpandButton).toggleExpand()
            }
        }
        synopsisBox = findViewById(R.id.story_box_synopsis_box)
        charactersBox = findViewById(R.id.story_box_characters_box)

        update()
    }

    fun update() {
        var hasAddedInfo = false

        storyTitleTextView.text = getStoryTitle(mStory)

        hasAddedInfo = updateSynopsis(hasAddedInfo)
        hasAddedInfo = updateCharacters(hasAddedInfo)
        updateExpandButton(hasAddedInfo)
        updateCredits()
    }

    private fun updateExpandButton(hasAddedInfo: Boolean) {
        if (!hasAddedInfo) {
            storyDetailButton.visibility = GONE
        } else {
            storyDetailButton.visibility = VISIBLE
        }
    }

    private fun updateCharacters(hasAddedInfo: Boolean): Boolean {
        var hasAddedInfo1 = hasAddedInfo
        if (mAppearances.filter { it.appearance.story == mStory.storyId }.isNotEmpty()) {
            hasAddedInfo1 = true
            charactersBox.visibility = VISIBLE
            val characters: TableLayout = findViewById(R.id.story_box_characters)

            mAppearances.forEach { appearance ->
                if (appearance.appearance.story == mStory.storyId) {
                    characters.addView(AppearanceRow(context, appearance, cb))
                }
            }
        } else {
            charactersBox.visibility = GONE
        }
        return hasAddedInfo1
    }

    private fun updateSynopsis(hasAddedInfo: Boolean): Boolean {
        var hasAddedInfo1 = hasAddedInfo
        if (mStory.synopsis?.isNotBlank() == true) {
            hasAddedInfo1 = true
            synopsisBox.visibility = VISIBLE
            val synopsis: TextView = findViewById(R.id.story_box_synopsis)
            synopsis.text = mStory.synopsis
        } else {
            synopsisBox.visibility = GONE
        }
        return hasAddedInfo1
    }

    private fun updateCredits() {
        val table = TableLayout(context, null)
        addView(table)
        mCredits.forEach { credit ->
            table.addView(CreditsRow(context, credit, cb))
        }
    }

    private fun getStoryTitle(story: Story) =
        if (story.storyType == STORY_TYPE_COVER) {
            "Cover ${
                story.title.let {
                    if (it != "") {
                        " - ${story.title}"
                    } else {
                        ""
                    }
                }
            }"
        } else {
            story.title.let {
                if (it == "") {
                    "Untitled Story"
                } else {
                    it
                }
            }
        }

    interface StoryBoxCallback {
        fun setExpanded(storyId: Int, isExpanded: Boolean)
    }
}

@SuppressLint("ViewConstructor")
@ExperimentalCoroutinesApi
class AppearanceRow(
    context: Context,
    private val fullCredit: FullAppearance,
    cb: CharacterLink.CharacterLinkCallback
) :
    TableRow(context) {

    init {
        inflate(context, R.layout.row_item_appearance, this)
        val charLink: CharacterLink = findViewById(R.id.appearance_row_character)
        val notes: TextView = findViewById(R.id.appearance_row_notes)
        val divider: View = findViewById(R.id.appearance_row_divider)
        val details: TextView = findViewById(R.id.appearance_row_details)
        val membership: TextView = findViewById(R.id.appearance_row_membership)

        charLink.apply {
            this.character = fullCredit.character
            this.callback = cb
        }

        fullCredit.appearance.notes?.let {
            notes.apply {
                text = it
                setTextAppearance(R.style.LinkTextView)
                visibility = VISIBLE
            }
        }

        fullCredit.appearance.details?.let {
            details.apply {
                text = it
                setTextAppearance(R.style.LinkTextView)
                visibility = VISIBLE
            }.also {
                divider.visibility = VISIBLE
            }
        }

        fullCredit.appearance.membership?.let {
            membership.apply {
                text = it
                setTextAppearance(R.style.LinkTextView)
                visibility = VISIBLE
            }
        }
    }
}

@ExperimentalCoroutinesApi
@SuppressLint("ViewConstructor")
class CreditsRow(
    context: Context,
    private val fullCredit: FullCredit,
    cb: CreatorLink.CreatorLinkCallback
) :
    TableRow(context) {

    init {
        this.addView(RoleNameTextView(context, fullCredit.role.roleName))
        this.addView(CreatorLink(context).apply {
            creator = fullCredit.nameDetail
            callback = cb
        })
    }
}




