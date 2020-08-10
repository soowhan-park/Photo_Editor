package com.example.photogallery.GoogleVisionUtils;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.photogallery.Activity.MainActivity;
import com.example.photogallery.R;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;

import java.io.IOException;
import java.lang.ref.WeakReference;

import static androidx.constraintlayout.widget.Constraints.TAG;
import static com.example.photogallery.Activity.MainActivity.convertResponseToString;

public class LabelDetectionTask extends AsyncTask<Object, Void, String> {
    private final WeakReference<MainActivity> mActivityWeakReference;
    private Vision.Images.Annotate mRequest;

    public LabelDetectionTask(MainActivity activity, Vision.Images.Annotate annotate) {
        mActivityWeakReference = new WeakReference<>(activity);
        mRequest = annotate;
    }

    public LabelDetectionTask(WeakReference<MainActivity> mActivityWeakReference) {
        this.mActivityWeakReference = mActivityWeakReference;
    }

    @Override
    protected String doInBackground(Object... params) {
        try {
            Log.d(TAG, "created Cloud Vision request object, sending request");
            BatchAnnotateImagesResponse response = mRequest.execute();
            return convertResponseToString(response);
        } catch (GoogleJsonResponseException e) {
            Log.d(TAG, "failed to make API request because " + e.getContent());
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
        return "Cloud Vision API request failed. Check logs for details.";
    }

    protected void onPostExecute(String result) {
        MainActivity activity = mActivityWeakReference.get();
        String[] gVision = result.split(",");
        if (activity != null && !activity.isFinishing()) {

            LinearLayout linearLayout = activity.findViewById(R.id.linearlayout);
            TextView imageDetail = activity.findViewById(R.id.image_details);
            SpinKitView loadView = activity.findViewById(R.id.spin_kit2);
            loadView.setVisibility(View.GONE);
            imageDetail.setVisibility(View.GONE);

            for (int i = 0; i< gVision.length; i++){
                CheckBox temp = new CheckBox(activity.getApplicationContext());
                temp.setText(gVision[i]);
                linearLayout.addView(temp);
            }
        }
    }
}
