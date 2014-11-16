package com.mde.potdroid.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.Spanned;
import android.view.*;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.mde.potdroid.EditorActivity;
import com.mde.potdroid.R;
import com.mde.potdroid.TopicActivity;
import com.mde.potdroid.helpers.AsyncHttpLoader;
import com.mde.potdroid.helpers.DatabaseWrapper;
import com.mde.potdroid.helpers.Utils;
import com.mde.potdroid.models.Board;
import com.mde.potdroid.models.Post;
import com.mde.potdroid.models.Topic;
import com.mde.potdroid.parsers.BoardParser;
import com.mde.potdroid.views.IconDrawable;
import org.apache.http.Header;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * The Board Fragment, which contains a list of Topics.
 */
public class BoardFragment extends PaginateFragment implements LoaderManager.LoaderCallbacks<Board> {

    // the tags of the fragment arguments
    public static final String ARG_ID = "board_id";
    public static final String ARG_PAGE = "page";
    // the board object
    private Board mBoard;
    // the topic list adapter
    private BoardListAdapter mListAdapter;
    private ListView mListView;
    // bookmark database handler
    private DatabaseWrapper mDatabase;

    /**
     * Returns an instance of the BoardFragment and sets required parameters as Arguments
     *
     * @param board_id the id of the board
     * @param page     the currently visible page
     * @return BoardFragment object
     */
    public static BoardFragment newInstance(int board_id, int page) {
        BoardFragment f = new BoardFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(ARG_ID, board_id);
        args.putInt(ARG_PAGE, page);

        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View v = inflater.inflate(R.layout.layout_board, container, false);

        mListAdapter = new BoardListAdapter();
        mListView = (ListView) v.findViewById(R.id.forum_list_content);
        mListView.setAdapter(mListAdapter);

        // clicking on a topic leads to the topicactivity
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getBaseActivity(), TopicActivity.class);
                intent.putExtra(TopicFragment.ARG_TOPIC_ID, mBoard.getFilteredTopics(getActivity()).get(position)
                        .getId());
                intent.putExtra(TopicFragment.ARG_PAGE, mBoard.getFilteredTopics(getActivity()).get(position)
                        .getNumberOfPages());
                startActivity(intent);
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getBaseActivity(), TopicActivity.class);
                intent.putExtra(TopicFragment.ARG_TOPIC_ID, mBoard.getFilteredTopics(getActivity()).get(position)
                        .getId());
                intent.putExtra(TopicFragment.ARG_PAGE, 1);
                startActivity(intent);
                return true;
            }
        });

        mDatabase = new DatabaseWrapper(getActivity());

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        if (mBoard == null)
            startLoader(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.actionmenu_board, menu);

        menu.findItem(R.id.new_thread).setIcon(IconDrawable.getIconDrawable(getActivity(), R.string.icon_pencil));

        if (!Utils.isLoggedIn()) {
            menu.setGroupVisible(R.id.loggedout_board, false);
        } else {
            menu.setGroupVisible(R.id.loggedout_board, true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.new_thread:
                newThread();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Open the form for a new thread
     */
    public void newThread() {
        if(mBoard == null)
            return;

        Intent intent = new Intent(getBaseActivity(), EditorActivity.class);
        intent.putExtra(EditorFragment.ARG_MODE, EditorFragment.MODE_THREAD);
        intent.putExtra(EditorFragment.ARG_BOARD_ID, mBoard.getId());
        intent.putExtra(EditorFragment.ARG_TOKEN, mBoard.getNewthreadtoken());

        startActivityForResult(intent, EditorFragment.MODE_THREAD);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == EditorFragment.MODE_THREAD) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(getBaseActivity(), TopicActivity.class);
                intent.putExtra(TopicFragment.ARG_TOPIC_ID, data.getExtras().getInt(EditorFragment.ARG_TOPIC_ID));
                intent.putExtra(TopicFragment.ARG_PAGE, 1);
                startActivity(intent);
            }
        }
    }

    @Override
    public Loader<Board> onCreateLoader(int id, Bundle args) {
        int page = getArguments().getInt(ARG_PAGE, 1);
        int bid = getArguments().getInt(ARG_ID, 0);

        showLoadingAnimation();

        return new AsyncContentLoader(getBaseActivity(), page, bid);
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        restartLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Board> loader, Board data) {
        hideLoadingAnimation();

        if (data != null) {
            mBoard = data;

            // refresh the list
            mListAdapter.notifyDataSetChanged();

            // refresh the OptionsMenu, because of new pagination possibilities
            //getBaseActivity().supportInvalidateOptionsMenu();
            refreshPaginateLayout();

            // generate subtitle and set title and subtitle of the actionbar
            Spanned subtitle = Html.fromHtml(String.format(getString(
                    R.string.subtitle_paginate), mBoard.getPage(),
                    mBoard.getNumberOfPages()));

            getActionbar().setTitle(mBoard.getName());
            getActionbar().setSubtitle(subtitle);

            // scroll to top
            mListView.setSelectionAfterHeaderView();

        } else {
            showError(getString(R.string.msg_loading_error));
        }
    }

    @Override
    public void onLoaderReset(Loader<Board> loader) {
        hideLoadingAnimation();
    }

    public void goToNextPage() {
        // whether there is a next page was already checked in onCreateOptionsMenu
        getArguments().putInt(ARG_PAGE, mBoard.getPage() + 1);
        restartLoader(this);
    }

    public void goToPrevPage() {
        // whether there is a previous page was already checked in onCreateOptionsMenu
        getArguments().putInt(ARG_PAGE, mBoard.getPage() - 1);
        restartLoader(this);
    }

    public void goToLastPage() {
        // whether there is a previous page was checked in onCreateOptionsMenu
        getArguments().putInt(ARG_PAGE, mBoard.getNumberOfPages());
        restartLoader(this);
    }

    public void goToFirstPage() {
        // whether there is a previous page was already checked in onCreateOptionsMenu
        getArguments().putInt(ARG_PAGE, 1);
        restartLoader(this);
    }

    @Override
    public boolean isLastPage() {
        return mBoard == null || mBoard.isLastPage();
    }

    @Override
    public boolean isFirstPage() {
        return mBoard == null || mBoard.getPage() == 1;
    }

    public void refreshPage() {
        restartLoader(this);
    }

    /**
     * The content loader
     */
    static class AsyncContentLoader extends AsyncHttpLoader<Board> {

        AsyncContentLoader(Context cx, int page, int board_id) {
            super(cx, BoardParser.getUrl(board_id, page));
        }

        @Override
        public Board processNetworkResponse(String response) {
            try {
                BoardParser parser = new BoardParser();
                return parser.parse(response);
            } catch (Exception e) {
                Utils.printException(e);
                return null;
            }
        }

        @Override
        protected void onNetworkFailure(int statusCode, Header[] headers,
                                        String responseBody, Throwable error) {

            Utils.printException(error);
            deliverResult(null);
        }

    }

    private class BoardListAdapter extends BaseAdapter {

        public int getCount() {
            if (mBoard == null)
                return 0;
            return mBoard.getFilteredTopics(getActivity()).size();
        }

        public Object getItem(int position) {
            return mBoard.getFilteredTopics(getActivity()).get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = getInflater().inflate(R.layout.listitem_thread, null);
            Topic t = (Topic) getItem(position);

            // set the name, striked if closed
            TextView title = (TextView) row.findViewById(R.id.title);
            title.setText(t.getTitle());
            if (t.isClosed())
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            // set the subtitle
            TextView subtitle = (TextView) row.findViewById(R.id.subtitle);
            subtitle.setText(t.getSubTitle());

            // pages information
            TextView pages = (TextView) row.findViewById(R.id.pages);
            Spanned pages_content = Html.fromHtml(String.format(getString(
                    R.string.topic_additional_information),
                    t.getNumberOfPosts(), t.getNumberOfPages()));
            pages.setText(pages_content);

            // lastpost
            Post displayPost;

            if(t.getLastPost() != null) {
                displayPost = t.getLastPost();
            } else {
                displayPost = t.getFirstPost();
            }
            TextView lastpost = (TextView) row.findViewById(R.id.author);
            String time = new SimpleDateFormat(getString(R.string.default_time_format)).format(displayPost.getDate());
            lastpost.setText(Html.fromHtml(String.format(
                    getString(R.string.thread_lastpost), displayPost.getAuthor().getNick(), time)));

            // icon
            if (t.getIconId() != null) {
                try {
                    Drawable d = Utils.getIcon(getActivity(), t.getIconId());
                    d.setBounds(0, 0, (int)title.getTextSize(), (int)title.getTextSize());
                    title.setCompoundDrawables(d, null, null, null);
                } catch (IOException e) {
                    Utils.printException(e);
                }
            }

            // all important topics get a different background.
            // the padding stuff is apparently an android bug...
            // see http://stackoverflow.com/questions/5890379
            if (t.isSticky() || t.isImportant() || t.isAnnouncement() || t.isGlobal()) {
                View v = row.findViewById(R.id.container);
                int padding_top = v.getPaddingTop();
                int padding_bottom = v.getPaddingBottom();
                int padding_right = v.getPaddingRight();
                int padding_left = v.getPaddingLeft();

                v.setBackgroundResource(Utils.getDrawableResourceIdByAttr(getActivity(), R.attr.bbBackgroundListActive));
                v.setPadding(padding_left, padding_top, padding_right, padding_bottom);
            }

            if (!t.isSticky()) {
                row.findViewById(R.id.icon_pinned).setVisibility(View.GONE);
            }

            if (!t.isClosed()) {
                row.findViewById(R.id.icon_locked).setVisibility(View.GONE);
            }

            if (!mDatabase.isBookmark(t)) {
                row.findViewById(R.id.icon_bookmarked).setVisibility(View.GONE);
            }

            return row;
        }
    }

}
