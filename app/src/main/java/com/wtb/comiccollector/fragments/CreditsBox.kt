package com.wtb.comiccollector.fragments

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FullAppearance
import com.wtb.comiccollector.database.models.FullCredit
import com.wtb.comiccollector.database.models.Story
import com.wtb.comiccollector.views.CharacterLink
import com.wtb.comiccollector.views.CharacterLinkCallback
import com.wtb.comiccollector.views.CreatorLink
import com.wtb.comiccollector.views.CreatorLinkCallback
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val STORY_TYPE_COVER = 6

@ExperimentalCoroutinesApi
class CreditsBox(context: Context) : TableLayout(context) {

    interface CreditsBoxCallback : CharacterLinkCallback, CreatorLinkCallback

    var mCallback: CreditsBoxCallback? = null
    private var mIssueStories: List<Story> = emptyList()
    private var mVariantStories: List<Story> = emptyList()
    private var mIssueCredits: List<FullCredit> = emptyList()
    private var mVariantCredits: List<FullCredit> = emptyList()
    private var mIssueAppearances: List<FullAppearance> = emptyList()
    private var mVariantAppearances: List<FullAppearance> = emptyList()

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
        displayCredit()
    }

    init {
        orientation = VERTICAL
        layoutParams =
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        isStretchAllColumns = true
    }

    private fun displayCredit() {
        this.removeAllViews()
        val stories = getCompleteVariantStories(mIssueStories, mVariantStories)
        stories.forEach { story ->
            this.addView(StoryRow(context, story))
            val credits = mIssueCredits + mVariantCredits
            credits.forEach { credit ->
                if (credit.story.storyId == story.storyId) {
                    this.addView(CreditsRow(context, credit))
                }
            }
        }
    }

    /**
     * Get complete variant stories - returns the full story list for the variant issues
     */
    private fun getCompleteVariantStories(
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


    inner class StoryRow(context: Context, mStory: Story) : LinearLayout(context) {
        init {
            val storyTitle1 = getStoryTitle(mStory)
            var hasAddedInfo = false

            orientation = VERTICAL
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT)

            inflate(context, R.layout.story_box, this)

            val storyDetailButton: ImageButton = findViewById(R.id.story_dropdown_button)
            val storyDetailBox: LinearLayout = findViewById(R.id.story_details_box)
            storyDetailButton.setOnClickListener {
                storyDetailBox.toggleVisibility()
                (it as ImageButton).toggleIcon(storyDetailBox)
            }

            val storyTitle = findViewById<TextView>(R.id.story_title)
            storyTitle.text = storyTitle1

            if (mStory.synopsis != null && mStory.synopsis != "") {
                hasAddedInfo = true
                val synopsis: TextView = findViewById(R.id.synopsis)
                synopsis.text = mStory.synopsis
            } else {
                val synopsisBox: TextView = findViewById(R.id.label_synopsis)
                val synopsis: TextView = findViewById(R.id.synopsis)
                synopsisBox.visibility = GONE
                synopsis.visibility = GONE
            }

            val appearances = mIssueAppearances + mVariantAppearances
            if (appearances.isNotEmpty()) {
                hasAddedInfo = true
                val characters: TableLayout = findViewById(R.id.characters)

                appearances.forEach { appearance ->
                    if (appearance.appearance.story == mStory.storyId) {
                        characters.addView(AppearanceRow(context, appearance))
                    }
                }
            } else {
                val characterBox: TextView = findViewById(R.id.label_characters)
                val characters: TableLayout = findViewById(R.id.characters)
                characterBox.visibility = GONE
                characters.visibility = GONE
            }

            if (!hasAddedInfo) {
                storyDetailButton.visibility = GONE
            } else {
                storyDetailButton.visibility = VISIBLE
            }
        }
    }

    private fun getStoryTitle(story: Story) = if (story.storyType == STORY_TYPE_COVER) {
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

    inner class CreditsRow(context: Context, private val fullCredit: FullCredit) :
        TableRow(context) {

        init {
            this.addView(RoleNameTextView(context, fullCredit.role.roleName))
            this.addView(CreatorLink(context).apply {
                creator = fullCredit.nameDetail
                callback = this@CreditsBox.mCallback
            })
        }
    }

    inner class AppearanceRow(context: Context, private val fullCredit: FullAppearance) :
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
                this.callback = this@CreditsBox.mCallback
            }

            fullCredit.appearance.notes?.let {
                notes.apply {
                    text = it
                    setTextAppearance(R.style.CreatorLink)
                    visibility = VISIBLE
                }
            }

            fullCredit.appearance.details?.let {
                details.apply {
                    text = it
                    setTextAppearance(R.style.CreatorLink)
                    visibility = VISIBLE
                }.also {
                    divider.visibility = VISIBLE
                }
            }

            fullCredit.appearance.membership?.let {
                membership.apply {
                    text = it
                    setTextAppearance(R.style.CreatorLink)
                    visibility = VISIBLE
                }
            }
        }
    }

    companion object {
        private const val TAG = APP + "CreditsBox"
    }
}