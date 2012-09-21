package com.bourke.glimmr.fragments.viewer;

import android.content.Context;

import android.os.AsyncTask;
import android.os.Bundle;

import android.text.Html;

import android.util.Log;

import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;

import com.bourke.glimmr.common.Constants;
import com.bourke.glimmr.common.PrettyDate;
import com.bourke.glimmr.event.Events.ICommentAddedListener;
import com.bourke.glimmr.event.Events.ICommentsReadyListener;
import com.bourke.glimmr.event.Events.IUserReadyListener;
import com.bourke.glimmr.fragments.base.BaseFragment;
import com.bourke.glimmr.R;
import com.bourke.glimmr.tasks.AddCommentTask;
import com.bourke.glimmr.tasks.LoadCommentsTask;
import com.bourke.glimmr.tasks.LoadUserTask;

import com.googlecode.flickrjandroid.people.User;
import com.googlecode.flickrjandroid.photos.comments.Comment;
import com.googlecode.flickrjandroid.photos.Photo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CommentsFragment extends BaseFragment
        implements ICommentsReadyListener, ICommentAddedListener,
                   IUserReadyListener {

    protected String TAG = "Glimmr/CommentsFragment";

    private LoadCommentsTask mTask;
    private Photo mPhoto = new Photo();
    private ArrayAdapter<Comment> mAdapter;
    private Map<String, UserItem> mUsers = Collections.synchronizedMap(
            new HashMap<String, UserItem>());
    private List<LoadUserTask> mLoadUserTasks = new ArrayList<LoadUserTask>();

    public static CommentsFragment newInstance(Photo photo) {
        CommentsFragment newFragment = new CommentsFragment();
        newFragment.mPhoto = photo;
        return newFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mLayout = (RelativeLayout) inflater.inflate(
                R.layout.comments_fragment, container, false);
        mAq = new AQuery(mActivity, mLayout);
        mAq.id(R.id.submitButton).clicked(this, "submitButtonClicked");
        mAq.id(R.id.progressIndicator).visible();
        return mLayout;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Constants.DEBUG) Log.d(TAG, "onPause");

        if (mTask != null) {
            mTask.cancel(true);
        }

        /* Also stop any remaining LoadUserTasks */
        for (AsyncTask loadUserTask : mLoadUserTasks) {
            loadUserTask.cancel(true);
        }
    }

    @Override
    protected void startTask() {
        super.startTask();
        if (Constants.DEBUG)
            Log.d(getLogTag(), "startTask()");
        mTask = new LoadCommentsTask(this, this, mPhoto);
        mTask.execute(mOAuth);
    }

    public void submitButtonClicked(View view) {
        TextView editText = (TextView) mLayout.findViewById(R.id.editText);
        String commentText = editText.getText().toString();
        if (commentText.isEmpty()) {
            // TODO: alert user
            if (Constants.DEBUG) {
                Log.d(getLogTag(), "Comment text empty, do nothing");
            }
            return;
        }

        if (Constants.DEBUG) {
            Log.d(getLogTag(), "Starting AddCommentTask: " + commentText);
        }
        new AddCommentTask(this, this, mPhoto, commentText)
            .execute(mOAuth);

        /* Clear the editText and hide keyboard */
        editText.setText("");
        InputMethodManager inputManager = (InputMethodManager)
            mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(mActivity.getCurrentFocus()
                .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        Toast.makeText(mActivity, mActivity.getString(R.string.comment_sent),
                Toast.LENGTH_SHORT).show();
    }

    public void itemClicked(AdapterView<?> parent, View view, int position,
            long id) {
        // TODO
    }

    @Override
    public void onUserReady(User user) {
        if (Constants.DEBUG)
            Log.d(getLogTag(), "onUserReady: " + user.getId());
        mUsers.put(user.getId(), new UserItem(user, false));
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCommentAdded(String commentId) {
        if (Constants.DEBUG) {
            Log.d(getLogTag(), "Sucessfully added comment with id: " +
                    commentId);
        }
        startTask();
    }

    @Override
    public void onCommentsReady(List<Comment> comments) {
        if (Constants.DEBUG) {
            Log.d(getLogTag(), "onCommentsReady, comments.size(): "
                + comments.size());
        }

        mAq.id(R.id.progressIndicator).gone();

        mAdapter = new ArrayAdapter<Comment>(mActivity,
                R.layout.comment_list_row, (ArrayList<Comment>) comments) {
            // TODO: implement ViewHolder pattern
            // TODO: add aquery delay loading for fling scrolling
            @Override
            public View getView(final int position, View convertView,
                    ViewGroup parent) {

                if (convertView == null) {
                    convertView = mActivity.getLayoutInflater().inflate(
                            R.layout.comment_list_row, null);
                }

                final Comment comment = getItem(position);
                AQuery aq = mAq.recycle(convertView);

                // TODO: if your username replace with "You"
                aq.id(R.id.userName).text(comment.getAuthorName());
                PrettyDate p = new PrettyDate(comment.getDateCreate());
                aq.id(R.id.commentDate).text(p.localisedPrettyDate(mActivity));

                aq.id(R.id.commentText).text(Html.fromHtml(comment.getText()));

                final UserItem author = mUsers.get(comment.getAuthor());
                if (author == null) {
                    mUsers.put(comment.getAuthor(), new UserItem(null, true));
                    LoadUserTask loadUserTask = new LoadUserTask(mActivity,
                            CommentsFragment.this, comment.getAuthor());
                    loadUserTask.execute(mOAuth);
                    mLoadUserTasks.add(loadUserTask);
                } else {
                    if (!author.isLoading) {
                        aq.id(R.id.userIcon).image(
                                author.user.getBuddyIconUrl(),
                                Constants.USE_MEMORY_CACHE,
                                Constants.USE_FILE_CACHE, 0, 0, null,
                                AQuery.FADE_IN_NETWORK);
                        aq.id(R.id.userIcon).clicked(
                                new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startProfileViewer(author.user);
                            }
                        });
                    }
                }
                return convertView;
            }
        };

        mAq.id(R.id.list).adapter(mAdapter).itemClicked(this,
                "itemClicked");
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    class UserItem {
        public User user;
        public boolean isLoading = true;

        public UserItem(User user, boolean isLoading) {
            this.user = user;
            this.isLoading = isLoading;
        }
    }
}
