package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.NameDetailAndCreator
import com.wtb.comiccollector.database.models.Series
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class CreatorLink(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.styleCreatorLink
) : AppCompatTextView(context, attrs, defStyleAttr), View.OnClickListener {

    internal var callback: CreatorLinkCallback? = null

    internal var creator: NameDetailAndCreator? = null
        set(value) {
            field = value
            val nameDetail = creator?.nameDetail?.name
            val creator = creator?.creator?.name
            val creditName = if (nameDetail != creator) {
                "$creator (as $nameDetail)"
            } else {
                creator
            }
            text = creditName
        }

    init {
        setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        Log.d(TAG, "Creator Clicked!!!!!")
        creator?.let { callback?.creatorClicked(it) }
    }

    companion object {
        private const val TAG = "CreatorLink"
    }
}

@ExperimentalCoroutinesApi
interface CreatorLinkCallback {
    fun creatorClicked(creator: NameDetailAndCreator)
}

@ExperimentalCoroutinesApi
class SeriesLink(
    context: Context,
    attrs: AttributeSet,
) : AppCompatTextView(context, attrs, R.attr.styleCreatorLink), View.OnClickListener {

    internal var callback: SeriesLinkCallback? = null

    internal var series: Series? = null
        set(value) {
            field = value
            text = series?.seriesName
        }

    init {
        setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        Log.d(TAG, "Creator Clicked!!!!!")
        series?.let { callback?.seriesClicked(it) }
    }

    companion object {
        private const val TAG = "CreatorLink"
    }
}

@ExperimentalCoroutinesApi
interface SeriesLinkCallback {
    fun seriesClicked(series: Series)
}