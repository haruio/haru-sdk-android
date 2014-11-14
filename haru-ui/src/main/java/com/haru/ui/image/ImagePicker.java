package com.haru.ui.image;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.haru.ui.R;
import com.haru.ui.image.cropper.CropImageActivity;
import com.haru.ui.image.workers.ImageProcessorListener;
import com.haru.ui.image.workers.ImageProcessorThread;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;

public class ImagePicker extends BChooser implements ImageProcessorListener {

    public static final int REQUEST_CROP = 6709;
    public static final int RESULT_ERROR = 404;

    private ImageChooserListener listener;

    private Context context;
    private Intent cropIntent;

    // menu titles
    private String title;
    private String titleGalleryOption;
    private String titleTakePictureOption;

    // to prevent the activity is being killed.
    private static HashMap<String, ImagePicker> chooserMap = new HashMap<String, ImagePicker>();

    /**
     *
     */
    public static class Builder {

        private ImagePicker picker;

        public Builder(Activity activity) {
            picker = new ImagePicker(activity,
                    activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath(), true);
            picker.context = activity;

            picker.title = activity.getString(R.string.haru_chooser_title);
            picker.titleGalleryOption = activity.getString(R.string.haru_chooser_gallery);
            picker.titleTakePictureOption = activity.getString(R.string.haru_chooser_take_picture);
        }

        public Builder setDialogTitle(String title) {
            picker.title = title;
            return this;
        }

        public Builder setDialogTitle(int resId) {
            picker.title = picker.context.getString(resId);
            return this;
        }

        public Builder crop(int widthRatio, int heightRatio) {
            picker.cropIntent = new Intent();
            picker.cropIntent.putExtra("aspect_x", widthRatio);
            picker.cropIntent.putExtra("aspect_y", heightRatio);
            return this;
        }

        public Builder cropWithMaxSize(int widthMaxPx, int heightMaxPx) {
            picker.cropIntent = new Intent();
            picker.cropIntent.putExtra("max_x", widthMaxPx);
            picker.cropIntent.putExtra("max_y", heightMaxPx);
            return this;
        }

        public Builder cropAsSquare() {
            picker.cropIntent = new Intent();
            picker.cropIntent.putExtra("aspect_x", 1);
            picker.cropIntent.putExtra("aspect_y", 1);
            return this;
        }

        public Builder cropAsCircle() {
            picker.cropIntent = new Intent();
            picker.cropIntent.putExtra("aspect_x", 1);
            picker.cropIntent.putExtra("aspect_y", 1);
            picker.cropIntent.putExtra("isCircleFocus", true);
            return this;
        }

        public ImagePicker build() {
            return picker;
        }
    }

    public ImagePicker(Activity activity, String foldername, boolean shouldCreateThumbnails) {
        super(activity, foldername, shouldCreateThumbnails);
        chooserMap.put(activity.getClass().getName(), this);
    }

    public ImagePicker(Fragment fragment, String foldername, boolean shouldCreateThumbnails) {
        super(fragment, foldername, shouldCreateThumbnails);
        chooserMap.put(fragment.getClass().getName(), this);
    }

    public ImagePicker(android.app.Fragment fragment, String foldername, boolean shouldCreateThumbnails) {
        super(fragment, foldername, shouldCreateThumbnails);
        chooserMap.put(fragment.getClass().getName(), this);
    }

    @Override
    public String choose() {
        String path = null;
        if (listener == null) {
            throw new IllegalArgumentException(
                    "ImageChooserListener cannot be null. Forgot to set ImageChooserListener???");
        }
        switch (type) {
            case ChooserType.REQUEST_PICK_PICTURE:
                choosePicture();
                break;
            case ChooserType.REQUEST_CAPTURE_PICTURE:
                path = takePicture();
                break;
            default:
                throw new IllegalArgumentException(
                        "Cannot choose a video in ImageChooserManager");
        }
        return path;
    }

    private void choosePicture()  {
        checkDirectory();
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            if (extras != null) {
                intent.putExtras(extras);
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            throw new RuntimeException("Activity not found");
        }
    }

    private String takePicture() {
        checkDirectory();
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            filePathOriginal = foldername
                    + File.separator + Calendar.getInstance().getTimeInMillis()
                    + ".jpg";
            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(filePathOriginal)));
            if (extras != null) {
                intent.putExtras(extras);
            }
            startActivity(intent);

        } catch (ActivityNotFoundException e) {
            throw new RuntimeException("Activity not found.");
        }
        return filePathOriginal;
    }

    @Override
    public void submit(int requestCode, Intent data) {
        switch (requestCode) {
            case ChooserType.REQUEST_PICK_PICTURE:
                processImageFromGallery(data);
                break;
            case ChooserType.REQUEST_CAPTURE_PICTURE:
                processCameraImage();
                break;
            case REQUEST_CROP:
                break;
        }
    }

    @SuppressLint("NewApi")
    private void processImageFromGallery(Intent data) {
        if (data != null && data.getDataString() != null) {
            String uri = data.getData().toString();
            sanitizeURI(uri);
            if (filePathOriginal == null || TextUtils.isEmpty(filePathOriginal)) {
                onError("File path was null");
            } else {
                String path = filePathOriginal;
                ImageProcessorThread thread = new ImageProcessorThread(path,
                        foldername, shouldCreateThumbnails);
                thread.setListener(this);
                if (activity != null) {
                    thread.setContext(activity.getApplicationContext());
                } else if (fragment != null) {
                    thread.setContext(fragment.getActivity()
                            .getApplicationContext());
                } else if (appFragment != null) {
                    thread.setContext(appFragment.getActivity()
                            .getApplicationContext());
                }
                thread.start();
            }
        } else {
            onError("Image Uri was null!");
        }

    }

    private void processCameraImage() {
        String path = filePathOriginal;
        ImageProcessorThread thread = new ImageProcessorThread(path,
                foldername, shouldCreateThumbnails);
        thread.setListener(this);
        thread.start();
    }

    @Override
    public void onError(String reason) {
        if (listener != null) {
            listener.onError(reason);
        }
    }

    // TODO: Dead problem
    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

        // get ImagePicker instance
        ImagePicker picker = chooserMap.get(activity.getClass().getName());
        if (picker == null) {
//            picker = new ImagePicker(activity, )

        }

        if (resultCode == Activity.RESULT_OK &&
                (requestCode == ChooserType.REQUEST_PICK_PICTURE ||
                 requestCode == ChooserType.REQUEST_CAPTURE_PICTURE)) {

//            submit(requestCode, data);
        } else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CROP) {
            // TODO: solve path problem
        }
    }

    Intent getCropIntent(Context context) {
        cropIntent.setClass(context, CropImageActivity.class);
        return cropIntent;
    }


    @Override
    public void onProcessedImage(ChosenImage image) {
        // Show cropper if needs
        if (cropIntent != null) {
            activity.startActivityForResult(getCropIntent(activity), REQUEST_CROP);

        } else if (listener != null) {
            listener.onImageChosen(image);
        }
    }



    public AlertDialog showPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        CharSequence[] titles = { titleGalleryOption, titleTakePictureOption };
        builder.setItems(titles, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                type = (which == 0 ? ChooserType.REQUEST_PICK_PICTURE
                        : ChooserType.REQUEST_CAPTURE_PICTURE);
                choose();
            }
        });

        return builder.show();
    }
}


