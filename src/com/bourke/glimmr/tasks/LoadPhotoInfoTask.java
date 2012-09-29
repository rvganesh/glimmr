package com.bourke.glimmrpro.tasks;

import android.os.AsyncTask;

import android.util.Log;

import com.bourke.glimmrpro.common.Constants;
import com.bourke.glimmrpro.common.FlickrHelper;
import com.bourke.glimmrpro.event.Events.IPhotoInfoReadyListener;
import com.bourke.glimmrpro.fragments.base.BaseFragment;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.photos.Photo;


public class LoadPhotoInfoTask extends AsyncTask<OAuth, Void, Photo> {

    private static final String TAG = "Glimmr/LoadPhotoInfoTask";

    private IPhotoInfoReadyListener mListener;
    private Photo mPhoto;
    private BaseFragment mBaseFragment;

    public LoadPhotoInfoTask(BaseFragment a, IPhotoInfoReadyListener listener,
            Photo photo) {
        mBaseFragment = a;
        mListener = listener;
        mPhoto = photo;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mBaseFragment.showProgressIcon(true);
    }

    @Override
    protected Photo doInBackground(OAuth... params) {
        OAuth oauth = params[0];
        if (oauth != null) {
            OAuthToken token = oauth.getToken();
            try {
                Flickr f = FlickrHelper.getInstance().getFlickrAuthed(
                        token.getOauthToken(), token.getOauthTokenSecret());
                return f.getPhotosInterface().getInfo(mPhoto.getId(),
                        mPhoto.getSecret());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            /* unauthenticated call */
            try {
                return FlickrHelper.getInstance().getPhotosInterface()
                    .getInfo(mPhoto.getId(), mPhoto.getSecret());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(final Photo result) {
        if (result == null) {
            if (Constants.DEBUG)
                Log.e(TAG, "Error fetching photo info, result is null");
        }
        mListener.onPhotoInfoReady(result);
        mBaseFragment.showProgressIcon(false);
    }

    @Override
    protected void onCancelled(final Photo result) {
        if (Constants.DEBUG)
            Log.d(TAG, "onCancelled");
    }
}
