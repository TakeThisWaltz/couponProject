package gun0912.tedbottompicker;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import gun0912.tedbottompicker.adapter.ImageGalleryAdapter;

public class TedBottomPicker extends BottomSheetDialogFragment {

    public static final String TAG = "TedBottomPicker";
    static final int REQ_CODE_CAMERA = 1;
    static final int REQ_CODE_GALLERY = 2;
    ImageGalleryAdapter imageGalleryAdapter;

    private Uri cameraImageUri;

    Builder builder;
    TextView tv_title;
    TextView tv_add_manual;
    private RecyclerView rc_gallery;
    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {


        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {

            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }

        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

        }
    };

    public void show(FragmentManager fragmentManager) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(this, getTag());
        ft.commitAllowingStateLoss();
    }

//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        Log.d(TAG, "onCreate - bundle : " + savedInstanceState + ", builder : " + builder);
//        super.onCreate(savedInstanceState);
//        if (savedInstanceState != null) {
//            if (savedInstanceState.containsKey("cameraImageUri"))
//                cameraImageUri = Uri.parse(savedInstanceState.getString("cameraImageUri"));
//            if(savedInstanceState.containsKey("builder")){
//                builder = (Builder) savedInstanceState.getSerializable("builder");
//            }
//        }
//
//        if (builder == null) builder = new Builder(this.getContext());
//    }
//
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        Log.d(TAG, "onCreateDialog - bundle : " + savedInstanceState + ", builder : " + builder);
//        return super.onCreateDialog(savedInstanceState);
//    }
//
//    @Override
//    public void onViewCreated(View contentView, @Nullable Bundle savedInstanceState) {
//        Log.d(TAG, "onViewCreated - bundle : " + savedInstanceState + ", builder : " + builder);
//        super.onViewCreated(contentView, savedInstanceState);
//    }
//
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        Log.d(TAG, "onSaveInstanceState - bundle : " + outState + ", builder : " + builder);
//        outState.putString("cameraImageUri", cameraImageUri.toString());
////        outState.putSerializable("builder", builder);
//    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory -  builder : " + builder);
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        Log.d(TAG, "setupDialog , builder : " + builder);
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.tedbottompicker_content_view, null);
        dialog.setContentView(contentView);
        CoordinatorLayout.LayoutParams layoutParams =
                (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = layoutParams.getBehavior();
        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
            if (builder.peekHeight > 0) {
                // ((BottomSheetBehavior) behavior).setPeekHeight(1500);
                ((BottomSheetBehavior) behavior).setPeekHeight(builder.peekHeight);
            }

        }

        rc_gallery = (RecyclerView) contentView.findViewById(R.id.rc_gallery);
        setRecyclerView();

        tv_title = (TextView) contentView.findViewById(R.id.tv_title);
        if (builder.onManualInputListener != null) {
            EditText etManualInput = new EditText(contentView.getContext());
            contentView.findViewById(R.id.tv_add_manual).setOnClickListener(v -> {
                new AlertDialog.Builder(contentView.getContext())
                        .setTitle(R.string.alert_input_barcode)
                        .setView(etManualInput)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                onManualInput(etManualInput.getText().toString());
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();
            });
        }
        setTitle();
    }


    private void setRecyclerView() {

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);
        rc_gallery.setLayoutManager(gridLayoutManager);


        rc_gallery.addItemDecoration(new GridSpacingItemDecoration(gridLayoutManager.getSpanCount(), builder.spacing, false));

        imageGalleryAdapter = new ImageGalleryAdapter(
                getActivity()
                , builder);
        rc_gallery.setAdapter(imageGalleryAdapter);
        imageGalleryAdapter.setOnItemClickListener(new ImageGalleryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                ImageGalleryAdapter.PickerTile pickerTile = imageGalleryAdapter.getItem(position);

                switch (pickerTile.getTileType()) {
                    case ImageGalleryAdapter.PickerTile.CAMERA:
                        startCameraIntent();
                        break;
                    case ImageGalleryAdapter.PickerTile.GALLERY:
                        startGalleryIntent();
                        break;
                    case ImageGalleryAdapter.PickerTile.IMAGE:
                        complete(pickerTile.getImageUri());

                        break;

                    default:
                        errorMessage();
                }

            }
        });
    }

    private void setTitle() {

        if (!builder.showTitle) {
            tv_title.setVisibility(View.GONE);
            return;
        }

        if (!TextUtils.isEmpty(builder.title)) {
            tv_title.setText(builder.title);
        }

        if (builder.titleBackgroundResId > 0) {
            tv_title.setBackgroundResource(builder.titleBackgroundResId);
        }

    }

    private void onManualInput(String code) {
        if (!TextUtils.isEmpty(code)) {
            builder.onManualInputListener.onManualInput(code);
        }
        dismiss();
    }

    private void complete(Uri uri) {
        //uri = Uri.parse(uri.toString());
        builder.onImageSelectedListener.onImageSelected(uri);
        dismiss();
    }

    private void startCameraIntent() {
        if (builder.onCameraSelectedListener != null)
            builder.onCameraSelectedListener.onCameraSelected();
        else {
            Intent cameraInent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraInent.resolveActivity(getActivity().getPackageManager()) == null) {
                errorMessage("This Application do not have Camera Application");
                return;
            }

            File imageFile = getImageFile();
            cameraInent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
            startActivityForResult(cameraInent, REQ_CODE_CAMERA);
        }
    }

    private void startGalleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (galleryIntent.resolveActivity(getActivity().getPackageManager()) == null) {
            errorMessage("This Application do not have Gallery Application");
            return;
        }

        startActivityForResult(galleryIntent, REQ_CODE_GALLERY);

    }

    private File getImageFile() {
        // Create an image file name
        File imageFile = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);


            imageFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );


            // Save a file: path for use with ACTION_VIEW intents
            cameraImageUri = Uri.fromFile(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage("Could not create imageFile for camera");
        }


        return imageFile;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImageUri = null;
            if (requestCode == REQ_CODE_GALLERY && data != null) {
                selectedImageUri = data.getData();
                if (selectedImageUri == null) {
                    errorMessage();
                }
            } else if (requestCode == REQ_CODE_CAMERA) {
                // Do something with imagePath
                selectedImageUri = cameraImageUri;
                MediaScannerConnection.scanFile(getContext(), new String[]{selectedImageUri.getPath()}, new String[]{"image/jpeg"}, null);
            }

            if (selectedImageUri != null) {
                complete(selectedImageUri);
            } else {
                errorMessage();
            }
        }

    }


    private void errorMessage() {
        errorMessage(null);
    }

    private void errorMessage(String message) {
        String errorMessage = message == null ? "Something wrong." : message;

        if (builder.onErrorListener == null) {
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
        } else {
            builder.onErrorListener.onError(errorMessage);
        }
    }


    public interface OnManualInputListener {
        void onManualInput(String code);
    }

    public interface OnImageSelectedListener {
        void onImageSelected(Uri uri);
    }

    public interface OnCameraSelectedListener {
        void onCameraSelected();
    }

    public interface OnErrorListener {
        void onError(String message);
    }

    public interface ImageProvider {
        void onProvideImage(ImageView imageView, Uri imageUri);
    }

    public static class Builder {

        public Context context;
        public int maxCount = 25;
        public Drawable cameraTileDrawable;
        public Drawable galleryTileDrawable;

        public int spacing = 1;

        public OnManualInputListener onManualInputListener;
        public OnImageSelectedListener onImageSelectedListener;
        public OnCameraSelectedListener onCameraSelectedListener;
        public OnErrorListener onErrorListener;

        public ImageProvider imageProvider;
        public boolean showCamera = true;
        public boolean showGallery = true;
        public int peekHeight = -1;
        public int cameraTileBackgroundResId = R.color.tedbottompicker_camera;
        public int galleryTileBackgroundResId = R.color.tedbottompicker_gallery;

        public String title;
        public boolean showTitle = true;
        public int titleBackgroundResId;

        public Builder(@NonNull Context context) {

            this.context = context;

            setCameraTile(R.drawable.ic_camera);
            setGalleryTile(R.drawable.ic_gallery);
            setSpacingResId(R.dimen.tedbottompicker_grid_layout_margin);
        }

        public Builder setMaxCount(int maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder setOnManualInputListener(OnManualInputListener onManualInputListener) {
            this.onManualInputListener = onManualInputListener;
            return this;
        }

        public Builder setOnImageSelectedListener(OnImageSelectedListener onImageSelectedListener) {
            this.onImageSelectedListener = onImageSelectedListener;
            return this;
        }

        public Builder setOnCameraSelectedListener(OnCameraSelectedListener onCameraSelectedListener) {
            this.onCameraSelectedListener = onCameraSelectedListener;
            return this;
        }

        public Builder setOnErrorListener(OnErrorListener onErrorListener) {
            this.onErrorListener = onErrorListener;
            return this;
        }

        public Builder showCameraTile(boolean showCamera) {
            this.showCamera = showCamera;
            return this;
        }

        public Builder setCameraTile(@DrawableRes int cameraTileResId) {
            setCameraTile(ContextCompat.getDrawable(context, cameraTileResId));
            return this;
        }

        public Builder setCameraTile(Drawable cameraTileDrawable) {
            this.cameraTileDrawable = cameraTileDrawable;
            return this;
        }

        public Builder showGalleryTile(boolean showGallery) {
            this.showGallery = showGallery;
            return this;
        }

        public Builder setGalleryTile(@DrawableRes int galleryTileResId) {
            setGalleryTile(ContextCompat.getDrawable(context, galleryTileResId));
            return this;
        }

        public Builder setGalleryTile(Drawable galleryTileDrawable) {
            this.galleryTileDrawable = galleryTileDrawable;
            return this;
        }

        public Builder setSpacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public Builder setSpacingResId(@DimenRes int dimenResId) {
            this.spacing = context.getResources().getDimensionPixelSize(dimenResId);
            return this;
        }

        public Builder setPeekHeight(int peekHeight) {
            this.peekHeight = peekHeight;
            return this;
        }

        public Builder setPeekHeightResId(@DimenRes int dimenResId) {
            this.peekHeight = context.getResources().getDimensionPixelSize(dimenResId);
            return this;
        }

        public Builder setCameraTileBackgroundResId(@ColorRes int colorResId) {
            this.cameraTileBackgroundResId = colorResId;
            return this;
        }

        public Builder setGalleryTileBackgroundResId(@ColorRes int colorResId) {
            this.galleryTileBackgroundResId = colorResId;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setTitle(@StringRes int stringResId) {
            this.title = context.getResources().getString(stringResId);
            return this;
        }

        public Builder showTitle(boolean showTitle) {
            this.showTitle = showTitle;
            return this;
        }

        public Builder setTitleBackgroundResId(@ColorRes int colorResId) {
            this.titleBackgroundResId = colorResId;
            return this;
        }

        public Builder setImageProvider(ImageProvider imageProvider) {
            this.imageProvider = imageProvider;
            return this;
        }


        public TedBottomPicker create() {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
//                    && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                throw new RuntimeException("Missing required WRITE_EXTERNAL_STORAGE permission. Did you remember to request it first?");
//            }

            if (onImageSelectedListener == null) {
                throw new RuntimeException("You have to setOnImageSelectedListener() for receive selected Uri");
            }

            TedBottomPicker customBottomSheetDialogFragment = new TedBottomPicker();

            customBottomSheetDialogFragment.builder = this;
            return customBottomSheetDialogFragment;
        }


    }


}
