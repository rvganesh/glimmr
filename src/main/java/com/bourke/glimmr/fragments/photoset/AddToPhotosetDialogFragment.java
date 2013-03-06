package com.bourke.glimmrpro.fragments.photoset;

import android.content.Intent;

import android.os.Bundle;

import android.support.v4.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bourke.glimmrpro.common.TaskQueueDelegateFactory;
import com.bourke.glimmrpro.common.TextUtils;
import com.bourke.glimmrpro.fragments.base.BaseDialogFragment;
import com.bourke.glimmrpro.fragments.home.PhotoStreamGridFragment;
import com.bourke.glimmrpro.R;
import com.bourke.glimmrpro.tape.AddToPhotosetTaskQueueService;
import com.bourke.glimmrpro.tasks.AddItemToPhotosetTask;

import com.googlecode.flickrjandroid.photosets.Photoset;
import com.googlecode.flickrjandroid.photos.Photo;

import com.squareup.tape.TaskQueue;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import java.util.List;

// TODO
// * Persist selections on rotate

/**
 * Present a dialog to that allows the user to select photos from their
 * photo stream to add to the current photoset.
 *
 * This is really just a host for the main fragments, we could make it an
 * activity but ABS doesn't support Activity dialog themes.
 */
public class AddToPhotosetDialogFragment extends BaseDialogFragment {

    public static final String TAG = "Glimmr/AddToPhotosetDialogFragment";

    public static final String QUEUE_FILE = "photoset_task_queue.json";

    private Photoset mPhotoset;
    private TaskQueue mQueue;
    private TextView mTitleView;
    private ProgressBar mProgressBar;

    public static AddToPhotosetDialogFragment newInstance(Photoset photoset) {
        AddToPhotosetDialogFragment newFragment =
            new AddToPhotosetDialogFragment();
        newFragment.mPhotoset = photoset;
        return newFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TaskQueueDelegateFactory<AddItemToPhotosetTask> factory =
            new TaskQueueDelegateFactory<AddItemToPhotosetTask>(mActivity);
        mQueue = new TaskQueue(factory.get(QUEUE_FILE,
                AddItemToPhotosetTask.class));
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mLayout = (LinearLayout) inflater.inflate(
                R.layout.add_to_photoset_fragment, container, false);

        mTitleView = (TextView) mLayout.findViewById(R.id.titleText);
        mTitleView.setText(R.string.add_photos);
        mTextUtils.setFont(mTitleView, TextUtils.FONT_ROBOTOBOLD);
        mProgressBar =
            (ProgressBar) mLayout.findViewById(R.id.progressIndicator);

        /* Nested fragments have to be added this way, not from xml */
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        final boolean retainInstance = false;
        final PhotoStreamGridFragment frag =
            PhotoStreamGridFragment.newInstance(retainInstance,
                    ListView.CHOICE_MODE_MULTIPLE);
        ft.replace(R.id.photoStreamFragment, frag);
        ft.commit();

        /* When add button is clicked, get selected ids and add to queue */
        mLayout.findViewById(R.id.buttonAddToPhotoset).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        List<Photo> selectedPhotos = frag.getSelectedPhotos();
                        for (Photo photo : selectedPhotos) {
                            mQueue.add(new AddItemToPhotosetTask(
                                    mPhotoset.getId(), photo.getId(), mOAuth));
                        }
                        mActivity.startService(new Intent(mActivity,
                                    AddToPhotosetTaskQueueService.class));
                        dismiss();
                        Crouton.makeText(mActivity,
                            R.string.photos_will_be_added,
                            Style.CONFIRM).show();
                    }
                });

        return mLayout;
    }
}