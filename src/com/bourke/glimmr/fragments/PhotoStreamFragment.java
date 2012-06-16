package com.bourke.glimmr;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.net.Uri;

import android.os.Bundle;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import com.androidquery.AQuery;

import com.gmail.yuyang226.flickr.oauth.OAuth;
import com.gmail.yuyang226.flickr.oauth.OAuthToken;
import com.gmail.yuyang226.flickr.people.User;
import com.gmail.yuyang226.flickr.photos.Photo;
import com.gmail.yuyang226.flickr.photos.PhotoList;

import java.util.ArrayList;

public class PhotoStreamFragment extends SherlockFragment
        implements IPhotoStreamReadyListener {

    private String TAG = "Glimmr/PhotoStreamFragment";

	private AQuery mGridAq;
    private Activity mActivity;
    private PhotoList mPhotos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getSherlockActivity();
    }

    public void onOAuthDone(OAuth result) {
        if (result == null) {
            Toast.makeText(mActivity, "Authorization failed",
                    Toast.LENGTH_LONG).show();
        } else {
            User user = result.getUser();
            OAuthToken token = result.getToken();
            if (user == null || user.getId() == null || token == null
                    || token.getOauthToken() == null
                    || token.getOauthTokenSecret() == null) {
                Toast.makeText(mActivity, "Authorization failed",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(mActivity, "Logged in!", Toast.LENGTH_LONG).show();
            saveOAuthToken(user.getUsername(), user.getId(),
                    token.getOauthToken(), token.getOauthTokenSecret());

			new LoadPhotostreamTask(this).execute(result);
        }
    }

    public void saveOAuthToken(String userName, String userId, String token,
            String tokenSecret) {
        Log.d(TAG, String.format("Saving userName=%s, userId=%s, " +
                    "oauth token=%s, and " + "token secret=%s", userName,
                    userId, token, tokenSecret));
        SharedPreferences sp = mActivity.getSharedPreferences(
                Constants.PREFS_NAME, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putString(Constants.KEY_OAUTH_TOKEN, token);
        editor.putString(Constants.KEY_TOKEN_SECRET, tokenSecret);
        editor.putString(Constants.KEY_USER_NAME, userName);
        editor.putString(Constants.KEY_USER_ID, userId);
        editor.commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = mActivity.getIntent();
        String scheme = intent.getScheme();
        OAuth savedToken = getOAuthToken();
        if (Constants.CALLBACK_SCHEME.equals(scheme) &&
                (savedToken == null || savedToken.getUser() == null)) {
            Uri uri = intent.getData();
            String query = uri.getQuery();
            Log.d(TAG, "Returned Query: " + query);
            String[] data = query.split("&");
            if (data != null && data.length == 2) {
                String oauthToken = data[0].substring(data[0].indexOf("=")
                        + 1);
                String oauthVerifier = data[1].substring(data[1].indexOf("=")
                        + 1);
                Log.d(TAG, String.format("OAuth Token: %s; OAuth Verifier: %s",
                            oauthToken, oauthVerifier));
                OAuth oauth = getOAuthToken();
                if (oauth != null && oauth.getToken() != null &&
                        oauth.getToken().getOauthTokenSecret() != null) {
                    GetOAuthTokenTask task = new GetOAuthTokenTask(this);
                    task.execute(oauthToken, oauth.getToken()
                            .getOauthTokenSecret(), oauthVerifier);
                }
            }
        }
    }

    private OAuth getOAuthToken() {
        /* Restore preferences */
        SharedPreferences settings = mActivity.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        String oauthTokenString = settings.getString(Constants.KEY_OAUTH_TOKEN,
                null);
        String tokenSecret = settings.getString(Constants.KEY_TOKEN_SECRET,
                null);
        if (oauthTokenString == null && tokenSecret == null) {
            Log.w(TAG, "No oauth token retrieved");
            return null;
        }

        OAuth oauth = new OAuth();
        String userName = settings.getString(Constants.KEY_USER_NAME, null);
        String userId = settings.getString(Constants.KEY_USER_ID, null);
        if (userId != null) {
            User user = new User();
            user.setUsername(userName);
            user.setId(userId);
            oauth.setUser(user);
        }

        OAuthToken oauthToken = new OAuthToken();
        oauth.setToken(oauthToken);
        oauthToken.setOauthToken(oauthTokenString);
        oauthToken.setOauthTokenSecret(tokenSecret);
        Log.d(TAG, String.format("Retrieved token from preference store: " +
                "oauth token=%s, and token secret=%s", oauthTokenString,
                tokenSecret));

        return oauth;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        RelativeLayout layout = (RelativeLayout) inflater.inflate(
                R.layout.gridview_fragment, container, false);

        OAuth oauth = getOAuthToken();
        if (oauth == null || oauth.getUser() == null) {
            OAuthTask task = new OAuthTask(this);
            task.execute();
        } else {
			new LoadPhotostreamTask(this).execute(oauth);
        }

        return layout;
    }

    @Override
    public void onPhotoStreamReady(PhotoList photos, boolean cancelled) {
		mGridAq = new AQuery(mActivity);
        mPhotos = photos;

		ArrayAdapter<Photo> adapter = new ArrayAdapter<Photo>(mActivity,
                R.layout.gridview_item, photos) {

            // TODO: implement ViewHolder pattern
			@Override
			public View getView(int position, View convertView,
                    ViewGroup parent) {

				if(convertView == null) {
					convertView = mActivity.getLayoutInflater().inflate(
                            R.layout.gridview_item, null);
				}

                Photo photo = getItem(position);
				AQuery aq = mGridAq.recycle(convertView);

                boolean useMemCache = true;
                boolean useFileCache = true;
                aq.id(R.id.image_item).image(photo.getLargeSquareUrl(),
                        useMemCache, useFileCache,  0, 0, null,
                        AQuery.FADE_IN_NETWORK);
                aq.id(R.id.viewsText).text("views:" + String.valueOf(photo
                            .getViews()));
                aq.id(R.id.ownerText).text(photo.getOwner().getUsername());

				return convertView;
			}
		};
        mGridAq.id(R.id.gridview).adapter(adapter).itemClicked(this,
                "startPhotoViewer");
		mGridAq.id(R.id.gridview).adapter(adapter);
    }

    /* The flickr Photo class isn't Serialisable, so construct a List of photo
     * urls to send it instead */
    public void startPhotoViewer(AdapterView parent, View v, int pos,
            long id) {
        ArrayList<String> photoUrls = new ArrayList<String>();
        for (Photo p : mPhotos) {
            photoUrls.add(p.getLargeUrl());
        }
        Log.d(TAG, "starting photo viewer with " + photoUrls.size() + " ids");
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.KEY_PHOTOVIEWER_LIST, photoUrls);
        Intent photoViewer = new Intent(mActivity, PhotoViewerActivity.class);
        photoViewer.putExtras(bundle);
        mActivity.startActivity(photoViewer);
    }
}
