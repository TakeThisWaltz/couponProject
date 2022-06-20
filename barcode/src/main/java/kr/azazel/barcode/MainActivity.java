package kr.azazel.barcode;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.azazel.framework.AzAppCompatActivity;
import com.azazel.framework.AzApplication;
import com.azazel.framework.AzSimpleWorker;
import com.azazel.framework.util.LOG;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.rey.material.widget.FloatingActionButton;
import com.theartofdev.edmodo.cropper.CropImageView;

import butterknife.ButterKnife;
import gun0912.tedbottompicker.TedBottomPicker;
import kr.azazel.barcode.adapters.ChannelPagerAdapter;
import kr.azazel.barcode.reader.BarcodeConvertor;
import kr.azazel.barcode.service.TextExtractorUtil;


public class MainActivity extends AzAppCompatActivity implements TedBottomPicker.OnImageSelectedListener, TedBottomPicker.OnCameraSelectedListener, TedBottomPicker.OnManualInputListener {
    public static final String TAG = "MainActivity";

    private static final int RC_BARCODE_CAPTURE = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_EXTAL_STORAGE_PERM = 3;


    private TedBottomPicker bottomPicker;
    private Toolbar toolbar;

    private FirebaseAnalytics mFirebaseAnalytics;

    private FloatingActionButton fab;

    @Override
    public boolean pauseListenWhenActivityOnPause() {
        return false;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public void onEventReceived(Message msg) {
        LOG.d(TAG, "onEventReceived - what : " + msg.what + ", obj : " + msg.obj);
        switch (msg.what) {
            case AzAppConstants.Event.TRY_EXIT: {
                LOG.i(TAG, "TRY_EXIT!!");
                exitFlag = false;
                break;
            }
            case AzAppConstants.Event.EXIT: {
                LOG.i(TAG, "EXIT!!");
                finish();
                break;
            }
            case AzAppConstants.Event.LISTVIEW_SCROLL_START:
                fab.animate().translationY(fab.getHeight() + 16).setInterpolator(new AccelerateInterpolator(2)).start();
                break;
            case AzAppConstants.Event.LISTVIEW_SCROLL_END:
                fab.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
                break;
        }
    }


    @Override
    public void onNewIntent(Intent intent) {
        LOG.i(TAG, "onNewIntent : " + intent);
        this.setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG.d(TAG, "onCreate - intent : " + getIntent());

        startActivity(new Intent(this, SplashActivity.class));

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        MobileAds.initialize(getApplicationContext(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                LOG.d(TAG, "onInitializationComplete - " + initializationStatus);
            }
        });
//        AdView mAdView = (AdView) findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder()
////                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // All emulators
////                .addTestDevice("65D277459E8AF4600A1B5729752CF637")  // This is my gal 6 device ID
//                .build();
//        mAdView.loadAd(adRequest);


        //((ImageView)findViewById(R.id.img_test)).setImageBitmap(BitmapFactory.decodeFile("/data/data/com.azazel.barcode/files/barcode_1"));

//        toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        ViewPager viewPager = (ViewPager) findViewById(R.id.vp_main);
        setupViewPager(viewPager);


        bottomPicker = new TedBottomPicker.Builder(MainActivity.this)
                .setOnManualInputListener(this)
                .setOnImageSelectedListener(this)
                .setOnCameraSelectedListener(this)
                .create();


        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                        && ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    final String[] permissions = new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};

                    ActivityCompat.requestPermissions(MainActivity.this, permissions, RC_HANDLE_EXTAL_STORAGE_PERM);
                    return;

                }

                if (!bottomPicker.isAdded())
                    bottomPicker.show(getSupportFragmentManager());
            }
        });


//        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
//        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
//            @Override
//            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
//                super.getItemOffsets(outRect, view, parent, state);
//                outRect.bottom = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
//            }
//        });


    }

    private void setupViewPager(final ViewPager viewPager) {
        final ChannelPagerAdapter adapter = new ChannelPagerAdapter(this);
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(R.mipmap.ic_total);
        tabLayout.getTabAt(1).setIcon(R.mipmap.ic_mem);
        tabLayout.getTabAt(2).setIcon(R.mipmap.ic_coupon);
        tabLayout.getTabAt(3).setIcon(R.mipmap.ic_etc2);

//        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//
//            }
//
//            @Override
//            public void onPageSelected(int position) {
//                toolbar.setTitle(adapter.getPageTitle(position));
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int state) {
//
//            }
//        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        LOG.d(TAG, "onResume - intent : " + getIntent());

        if (getIntent() != null) {
            Intent intent = getIntent();
            if (intent.getType() != null && intent.getType().contains("image")) {
                setIntent(null);
                Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

                LOG.d(TAG, "received image : " + uri);

                onImageSelected(uri);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void makeBarcodeUi(String barcodeValue, String barcodeFormat) {
//        Barcode barcode = BarcodeConvertor.convertZXingToGoogleType(barcodeValue, barcodeFormat);
//
//        makeBarcodeImage(null, barcode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOG.d(TAG, "onActivityResult - req : " + requestCode + ", res : " + resultCode + ", data : " + data);


        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                //Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                LOG.d(TAG, "scanned - " + result);

                if (result.getContents().startsWith("http://") || result.getContents().startsWith("https://")) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.alert_open_link)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getContents()));
                                    startActivity(browserIntent);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    makeBarcodeUi(result.getContents(), result.getFormatName());
                                }
                            })
                            .create().show();


                } else {
                    makeBarcodeUi(result.getContents(), result.getFormatName());
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }


//        if (requestCode == RC_BARCODE_CAPTURE) {
//            if (resultCode == CommonStatusCodes.SUCCESS) {
//                if (data != null) {
//                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
//
////                    Toast.makeText(MainActivity.this, "Code : " + barcode.displayValue + ", type : " + barcode.valueFormat, Toast.LENGTH_LONG).show();
////
////                    saveBarcode(barcode);
//                    Log.d(TAG, "detected barcode : " + barcode.rawValue + ", type : " + barcode.format);
//
//                    makeBarcodeImage(null, barcode);
//                } else {
//                    Log.d(TAG, "No barcode captured, intent data is null");
//                }
//            } else {
//                Log.d(TAG, String.format(getString(R.string.barcode_error),
//                        CommonStatusCodes.getStatusCodeString(resultCode)));
//            }
//        }
//        else {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
    }


//    private void saveBarcode(Barcode code){
//        Bitmap bitmap = BarcodeConvertor.getBitmap(code.rawValue, code.format, 1200, 500);
//
//        BarcodeConvertor.saveBitmaptoJpeg(bitmap, "Azazel", code.displayValue);
//    }

    @Override
    public void onImageSelected(Uri uri) {
//                        Snackbar.make(view, "URL : " + uri, Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
        if (uri != null) {

            AzApplication.executeJobOnBackground(new AzSimpleWorker() {
                @Override
                public void doInBackgroundAndResult() {
                    BarcodeConvertor.detectBarcode(MainActivity.this, uri, (detected)->{
//                        makeBarcodeImage(uri, detected);
                        setResult(true, detected);
                    });

                }

                @Override
                public void postOperationWithResult(boolean result, Object value) {
                    makeBarcodeImage(uri, (Barcode) value);
                }
            });

//            BarcodeConvertor.detectBarcode(MainActivity.this, uri, (detected)->{
//                makeBarcodeImage(uri, detected);
//            });

        }
    }

    @Override
    public void onCameraSelected() {
//        Intent intent = new Intent(MainActivity.this, BarcodeCaptureActivity.class);
//        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
//        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

        //startActivityForResult(intent, RC_BARCODE_CAPTURE);
        new IntentIntegrator(this).setBeepEnabled(false).initiateScan();
        bottomPicker.dismiss();
    }


    private void makeBarcodeImage(final Uri org, final Barcode code) {
        if (code != null) {
            final Bitmap bitmap = BarcodeConvertor.getBitmap(code.getRawValue(), code.getFormat(), AzAppConstants.BARCODE_IMG_WIDTH, AzAppConstants.BARCODE_IMG_HEIGHT);

            if (org != null) {

                TextExtractorUtil.extractExpireDateInBackground(org);

                PopupUtil.showCoverImageCropPopup(this, org, new CropImageView.OnCropImageCompleteListener() {
                    @Override
                    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {

                        PopupUtil.showNewBarcodePopup(MainActivity.this, org, code, bitmap, (result != null ? result.getBitmap() : null));
                    }
                });
            } else {
                PopupUtil.showNewBarcodePopup(MainActivity.this, org, code, bitmap, null);
            }
        } else {
            Toast.makeText(this, R.string.toast_no_barcode, Toast.LENGTH_LONG).show();
        }
    }

    private boolean exitFlag = false;

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (exitFlag == false) {
                Toast.makeText(this, R.string.toast_exit,
                        Toast.LENGTH_SHORT).show();
                exitFlag = true;
                AzApplication.sendEmptyMessageDelayed(TAG, AzAppConstants.Event.TRY_EXIT, 2000);
                return false;
            } else {
                finish();
                return false;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_EXTAL_STORAGE_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "permission granted - initialize the camera source");

            if (bottomPicker != null)
                bottomPicker.show(getSupportFragmentManager());
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));


    }

    @Override
    public void onManualInput(String code) {
        if (!TextUtils.isEmpty(code)) {
            makeBarcodeUi(code, "CODE_128");
        }
    }
}
