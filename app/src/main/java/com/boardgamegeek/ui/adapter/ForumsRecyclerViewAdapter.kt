package com.boardgamegeek.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.ui.ForumActivity
import kotlinx.android.synthetic.main.row_forum.view.*
import kotlinx.android.synthetic.main.row_forum_header.view.*
import java.text.NumberFormat

private const val ITEM_VIEW_TYPE_FORUM = 0
private const val ITEM_VIEW_TYPE_HEADER = 1

class ForumsRecyclerViewAdapter(private val gameId: Int, private val gameName: String?) : RecyclerView.Adapter<ForumViewHolder>() {
    init {
        setHasStableIds(true)
    }

    var forums: List<ForumEntity> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_VIEW_TYPE_FORUM -> ForumViewHolder.ForumItemViewHolder(inflater.inflate(R.layout.row_forum, parent, false))
            ITEM_VIEW_TYPE_HEADER -> ForumViewHolder.HeaderViewHolder(inflater.inflate(R.layout.row_forum_header, parent, false))
            else -> ForumViewHolder.HeaderViewHolder(inflater.inflate(R.layout.row_header, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
        return when (holder) {
            is ForumViewHolder.HeaderViewHolder -> holder.bind(forums.getOrNull(position))
            is ForumViewHolder.ForumItemViewHolder -> holder.bind(forums.getOrNull(position), gameId, gameName)
        }
    }

    override fun getItemCount() = forums.size

    override fun getItemViewType(position: Int): Int {
        val forum = forums.getOrNull(position)
        return if (forum?.isHeader != false) ITEM_VIEW_TYPE_HEADER else ITEM_VIEW_TYPE_FORUM
    }

    override fun getItemId(position: Int) = position.toLong()
}

private val numberFormat = NumberFormat.getNumberInstance()

sealed class ForumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class ForumItemViewHolder(itemView: View) : ForumViewHolder(itemView) {

        fun bind(forum: ForumEntity?, gameId: Int, gameName: String?) {
            if (forum == null) return
            itemView.apply {
                title.text = forum.title
                numberOfThreads.text = numberFormat.format(forum.numberOfThreads.toLong())
                lastPostDate.timestamp = forum.lastPostDateTime
                setOnClickListener { ForumActivity.start(it.context, forum.id, forum.title, gameId, gameName) }
            }
        }
    }

    class HeaderViewHolder(itemView: View) : ForumViewHolder(itemView) {
        fun bind(forum: ForumEntity?) {
            itemView.header.text = forum?.title
        }
    }
}


