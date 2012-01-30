package com.janoliver.potdroid.helpers;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import com.janoliver.potdroid.activities.TopicActivity;
import com.janoliver.potdroid.models.Post;
import com.janoliver.potdroid.models.Topic;

public class PostDialogs {

    public static class PostWriter extends AsyncTask<Object, Object, Boolean> {

        private ProgressDialog mDialog;
        private TopicActivity mActivity;
        private WebsiteInteraction mWebsiteInteraction;

        public PostWriter(TopicActivity act) {
            super();
            mActivity = act;
            mWebsiteInteraction = PotUtils.getWebsiteInteractionInstance(act);
        }

        @Override
        protected void onPreExecute() {
            mDialog = new PotNotification(mActivity, this, false);
            mDialog.setMessage("Sende Post...");
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            Topic thread          = (Topic)         params[0];
            DialogWrapper content = (DialogWrapper) params[1];
            
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("SID", ""));
            nameValuePairs.add(new BasicNameValuePair("PID", ""));
            nameValuePairs.add(new BasicNameValuePair("token", thread.getNewreplytoken()));
            nameValuePairs.add(new BasicNameValuePair("TID", "" + thread.getId()));
            nameValuePairs.add(new BasicNameValuePair("post_title", content.getTitle()));
            nameValuePairs.add(new BasicNameValuePair("message", content.getText()));
            nameValuePairs.add(new BasicNameValuePair("submit", "Eintragen"));

            return mWebsiteInteraction.sendPost(PotUtils.BOARD_URL_POST, nameValuePairs);
        }

        @Override
        protected void onCancelled() {
            mDialog.dismiss();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Toast.makeText(mActivity, "Fehlgeschlagen.", Toast.LENGTH_SHORT).show();
            } else {
                mActivity.refresh();
            }
            mDialog.dismiss();
        }
    }

    public static class PostEditer extends AsyncTask<Object, Object, Boolean> {

        private ProgressDialog mDialog;
        private TopicActivity mActivity;
        private WebsiteInteraction mWebsiteInteraction;

        public PostEditer(TopicActivity act) {
            super();
            mActivity = act;
            mWebsiteInteraction = PotUtils.getWebsiteInteractionInstance(act);
        }

        @Override
        protected void onPreExecute() {
            mDialog = new PotNotification(mActivity, this, false);
            mDialog.setMessage("Sende Post...");
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            Topic thread = (Topic) params[0];
            DialogWrapper content = (DialogWrapper) params[1];
            Post post = (Post) params[2];
            String token = null;
            
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            // nameValuePairs.add(new BasicNameValuePair("SID", ""));
            nameValuePairs.add(new BasicNameValuePair("PID", "" + post.getId()));
            token = post.getEdittoken();
            nameValuePairs.add(new BasicNameValuePair("token", token));
            nameValuePairs.add(new BasicNameValuePair("TID", "" + thread.getId()));
            nameValuePairs.add(new BasicNameValuePair("edit_title", content.getTitle()));
            nameValuePairs.add(new BasicNameValuePair("message", content.getText()));
            nameValuePairs.add(new BasicNameValuePair("submit", "Eintragen"));
            
            return mWebsiteInteraction.sendPost(PotUtils.BOARD_URL_EDITPOST, nameValuePairs);
        }

        @Override
        protected void onCancelled() {
            mDialog.dismiss();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Toast.makeText(mActivity, "Fehlgeschlagen.", Toast.LENGTH_SHORT).show();
            } else {
                mActivity.refresh();
            }
            mDialog.dismiss();
        }
    }
}
