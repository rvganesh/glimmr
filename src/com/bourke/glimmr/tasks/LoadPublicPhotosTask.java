package com.bourke.glimmr.tasks;

import android.os.AsyncTask;

import android.util.Log;

import com.bourke.glimmr.common.Constants;
import com.bourke.glimmr.common.FlickrHelper;
import com.bourke.glimmr.event.Events.IPhotoListReadyListener;
import com.bourke.glimmr.fragments.base.BaseFragment;

import com.googlecode.flickrjandroid.photos.Photo;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoadPublicPhotosTask extends AsyncTask<Void, Void, List<Photo>> {

    private static final String TAG = "Glimmr/LoadPublicPhotosTask";

    private IPhotoListReadyListener mListener;
    private BaseFragment mBaseFragment;
    private int mPage;

    public LoadPublicPhotosTask(BaseFragment a,
            IPhotoListReadyListener listener, int page) {
        mListener = listener;
        mBaseFragment = a;
        mPage = page;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mBaseFragment.showProgressIcon(true);
    }

    @Override
    protected List<Photo> doInBackground(Void... arg0) {
        if (Constants.DEBUG) Log.d(TAG, "Fetching page " + mPage);

        /* A specific date to return interesting photos for. */
        Date day = null;
        Set<String> extras = new HashSet<String>();
        extras.add("owner_name");
        extras.add("url_q");
        extras.add("url_l");
        extras.add("views");

        try {
            return FlickrHelper.getInstance().getInterestingInterface()
                .getList(day, extras, Constants.FETCH_PER_PAGE, mPage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(final List<Photo> result) {
        if (result == null) {
            if (Constants.DEBUG) {
                Log.e(TAG, "Error fetching photolist, result is null");
            }
        }
        mListener.onPhotosReady(result);
        mBaseFragment.showProgressIcon(false);
    }

    @Override
    protected void onCancelled(final List<Photo> result) {
        if (Constants.DEBUG) Log.d(TAG, "onCancelled");
    }
}
