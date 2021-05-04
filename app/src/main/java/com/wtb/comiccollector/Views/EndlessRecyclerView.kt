package com.wtb.comiccollector.Views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.Daos.REQUEST_LIMIT

private const val TAG = APP + "EndlessRecyclerView"

class EndlessRecyclerView(context: Context, attributeSet: AttributeSet) :
    RecyclerView(context, attributeSet) {

    private var loading: Boolean = true
    private var previousTotal: Int = 0
    private var visibleThreshold: Int = REQUEST_LIMIT / 2
    private var firstVisibleItem: Int = 0
    private var visibleItemCount: Int = 0
    private var totalItemCount: Int = 0
    var callbacks: Callbacks? = null

    init {
        this.layoutManager = LinearLayoutManager(context)

        this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                visibleItemCount = this@EndlessRecyclerView.childCount

                (layoutManager as LinearLayoutManager?)?.let {
                    totalItemCount = it.itemCount
                    firstVisibleItem = it.findFirstVisibleItemPosition()
                }

                if (loading) {
                    if (totalItemCount >= previousTotal) {
                        loading = false
                        previousTotal = totalItemCount
                    }
                }
                if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem +
                            visibleThreshold)
                ) {
                    Log.d(TAG, "End of list!")
                    callbacks?.getMore()
                    loading = true
                }
            }
        })
    }

    interface Callbacks {
        fun getMore()
    }
}