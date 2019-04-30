package com.boardgamegeek.ui.adapter;


import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.ForumEntity.ForumType;
import com.boardgamegeek.model.Thread;
import com.boardgamegeek.ui.ThreadActivity;
import com.boardgamegeek.ui.model.PaginatedData;
import com.boardgamegeek.ui.widget.TimestampView;

import java.text.NumberFormat;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ForumRecyclerViewAdapter extends PaginatedRecyclerViewAdapter<Thread> {
	private final int forumId;
	private final String forumTitle;
	private final int objectId;
	private final String objectName;
	private final ForumType objectType;
	private final NumberFormat numberFormat;

	public ForumRecyclerViewAdapter(Context context, PaginatedData<Thread> data, int forumId, String forumTitle, int objectId, String objectName, ForumType objectType) {
		super(context, R.layout.row_forum_thread, data);
		this.forumId = forumId;
		this.forumTitle = forumTitle;
		this.objectId = objectId;
		this.objectName = objectName;
		this.objectType = objectType;
		numberFormat = NumberFormat.getNumberInstance();
	}

	@NonNull
	@Override
	protected PaginatedItemViewHolder getViewHolder(View itemView) {
		return new ThreadViewHolder(itemView);
	}

	public class ThreadViewHolder extends PaginatedItemViewHolder {
		@BindView(R.id.subject) TextView subjectView;
		@BindView(R.id.author) TextView authorView;
		@BindView(R.id.number_of_articles) TextView numberOfArticlesView;
		@BindView(R.id.last_post_date) TimestampView lastPostDateView;

		public ThreadViewHolder(View view) {
			super(view);
			ButterKnife.bind(this, view);
		}

		@Override
		protected void bind(final Thread thread) {
			final Context context = itemView.getContext();
			subjectView.setText(thread.subject.trim());
			authorView.setText(thread.author);
			int replies = thread.numberOfArticles - 1;
			numberOfArticlesView.setText(numberFormat.format(replies));
			lastPostDateView.setTimestamp(thread.lastPostDate());
			itemView.setOnClickListener(v -> ThreadActivity.start(context, thread.id, thread.subject, forumId, forumTitle, objectId, objectName, objectType));
		}
	}
}
