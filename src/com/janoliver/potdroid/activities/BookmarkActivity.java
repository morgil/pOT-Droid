/*
 * Copyright (C) 2011 Jan Oliver Oelerich <janoliver@oelerich.org>
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this software, and changing it is allowed as long as the 
 * name is changed.
 *
 *           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *  TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *  0. You just DO WHAT THE FUCK YOU WANT TO. 
 */

package com.janoliver.potdroid.activities;

import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.janoliver.potdroid.R;
import com.janoliver.potdroid.baseclasses.BaseListActivity;
import com.janoliver.potdroid.helpers.ObjectManager.ParseErrorException;
import com.janoliver.potdroid.helpers.PotNotification;
import com.janoliver.potdroid.helpers.PotUtils;
import com.janoliver.potdroid.models.Bookmark;

/**
 * This Activity shows the bookmark list and handles all it's actions.
 */
public class BookmarkActivity extends BaseListActivity {

    /**
     * mBookmarks is a Map with all the bookmarks stored as 
     * <Id, BookmarkObject> values.
     */
    private Map<Integer, Bookmark> mBookmarks;

    /**
     * Starting point of the activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // check the login status and redirect in case the user is not logged in.
        PotUtils.log("luklz");
        if (!mObjectManager.isLoggedIn()) {
            finish();
            Intent intent = new Intent(BookmarkActivity.this, ForumActivity.class);
            intent.putExtra("noredirect", true);
            startActivityForResult(intent, 1);
            return;
        }

        // set view
        @SuppressWarnings("unchecked")
        final Map<Integer, Bookmark> stateSaved = (Map<Integer, Bookmark>) getLastNonConfigurationInstance();
        if (stateSaved == null) {
            setListAdapter(null);
            new PrepareAdapter().execute((Void[]) null);
        } else {
            mBookmarks = stateSaved;
            fillView();
        }
        
        // register context menu and the clicklistener
        registerForContextMenu(mListView);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    openThread((Bookmark)mBookmarks.values().toArray()[position -1], false, true);
                }
            }
        });
    }

    /**
     * After having downloaded the data, fill the view
     */
    private void fillView() {
        BookmarkViewAdapter adapter = new BookmarkViewAdapter(BookmarkActivity.this);
        mListView.addHeaderView(getHeaderView());
        mListView.setAdapter(adapter);

        setTitle("Bookmarks (" + mObjectManager.getUnreadBookmarks() + " neue Posts)");
    }

    /**
     * Needed for orientation change
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        final Map<Integer, Bookmark> stateSaved = mBookmarks;
        return stateSaved;
    }

    /**
     * Open a thread after a click on a bookmark.
     */
    public void openThread(Bookmark bm, Boolean lastPage, Boolean scroll) {
        Intent intent = new Intent(BookmarkActivity.this, TopicActivity.class);
        intent.putExtra("TID", bm.getThread().getId());
        
        if (scroll) {
            intent.putExtra("PID", bm.getLastPost().getId());
        }

        if (lastPage) {
            intent.putExtra("page", bm.getThread().getLastPage());
        }

        startActivity(intent);
    }
    
    /**
     * context menu creator
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notifications", false)) {
            inflater.inflate(R.menu.context_bookmark, menu);
        } else {
            inflater.inflate(R.menu.context_bookmark, menu);
        } 
    }
    
    /**
     * context menu item selected.
     * removebookmark could show a loading animation.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.first_page:
            openThread((Bookmark)mBookmarks.values().toArray()[(int) info.id], false, false);
            return true;
        case R.id.last_page:
            openThread((Bookmark)mBookmarks.values().toArray()[(int) info.id], true, false);
            return true;
        case R.id.removebookmark:
            // bookmark
            Bookmark bm = (Bookmark)mBookmarks.values().toArray()[(int) info.id];
            final String url = PotUtils.ASYNC_URL + "remove-bookmark.php?BMID=" + bm.getId()
                    + "&token=" + bm.getRemovetoken();
            new Thread(new Runnable() {
                public void run() {
                    mWebsiteInteraction.callPage(url);
                    BookmarkActivity.this.refresh();
                }
            }).start();
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Returns the header view for the list.
     */
    public View getHeaderView() {
        LayoutInflater inflater = this.getLayoutInflater();
        View row = inflater.inflate(R.layout.header_general, null);

        TextView descr = (TextView) row.findViewById(R.id.pagetext);
        descr.setText("Bookmarks: " + mBookmarks.size());

        TextView loggedin = (TextView) row.findViewById(R.id.loggedin);
        loggedin.setText(mObjectManager.isLoggedIn() ? "Hallo "
                + mObjectManager.getCurrentUser().getNick() : "nicht eingeloggt");

        return (row);
    }

    /**
     * refreshes the activity
     */
    @Override
    public void refresh() {
        Intent intent = new Intent(BookmarkActivity.this, BookmarkActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Custom view adapter for the ListView items
     */
    class BookmarkViewAdapter extends ArrayAdapter<Bookmark> {
        Activity context;

        BookmarkViewAdapter(Activity context) {
            super(context, R.layout.listitem_thread, R.id.name, 
                    mBookmarks.values().toArray(new Bookmark[0]));
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();

            View row = inflater.inflate(R.layout.listitem_bookmark, null);
            Bookmark bm = (Bookmark)mBookmarks.values().toArray()[position];

            TextView name = (TextView) row.findViewById(R.id.name);
            name.setText(bm.getThread().getTitle());
            TextView descr = (TextView) row.findViewById(R.id.description);
            descr.setText("Neue Posts: " + bm.getNumberOfNewPosts());
            TextView important = (TextView) row.findViewById(R.id.important);
            
            // red line when unread posts
            if (bm.getNumberOfNewPosts() > 0) {
                important.setBackgroundResource(R.color.darkred);
            }

            return (row);
        }
    }

    /**
     * This async task shows a loader and updates the bookmark object.
     * When it is finished, the loader is hidden.
     */
    class PrepareAdapter extends AsyncTask<Void, Void, Void> {
        ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            mDialog = new PotNotification(BookmarkActivity.this, this, true);
            mDialog.setMessage("Lade...");
            mDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mBookmarks = mObjectManager.getBookmarks();
            } catch (ParseErrorException e) {
                Toast.makeText(BookmarkActivity.this, "Verbindungsfehler!", Toast.LENGTH_LONG).show();
                this.cancel(true);
                mDialog.dismiss();
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void nix) {
            fillView();
            mDialog.dismiss();
        }
    }
}