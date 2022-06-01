package kr.azazel.barcode.controllers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.SparseArray;

import com.azazel.framework.util.LOG;

import kr.azazel.barcode.AzAppConstants;
import kr.azazel.barcode.local.AzAppDataHelper;
import kr.azazel.barcode.vo.MyBarcode;

/**
 * Created by JJ_Air on 2015-06-12.
 */
public class MainController extends ContentProvider {
    private static final String TAG = "MainController";

    private AzAppDataHelper mDataMgr;

    private UriMatcher mUriMatcher;

    private SparseArray<IRSController> mControllerMap;

    @Override
    public boolean onCreate() {
        LOG.i(TAG, "onCreate");
        mDataMgr = AzAppDataHelper.getInstance(this.getContext());

        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AzAppConstants.URI.AHTHORITY, "barcodes", 1);
        mUriMatcher.addURI(AzAppConstants.URI.AHTHORITY, "barcodes/#", 2);
        mUriMatcher.addURI(AzAppConstants.URI.AHTHORITY, "categories", 3);
        mUriMatcher.addURI(AzAppConstants.URI.AHTHORITY, "categories/#/barcodes", 4);

        mControllerMap = new SparseArray<IRSController>();
        mControllerMap.put(1, new IRSController() {
            @Override
            public Cursor query(Uri uri) {
                return mDataMgr.queryBarcodes();
            }
        });
        mControllerMap.put(2, new IRSController() {
            @Override
            public Cursor query(Uri uri) {
                return mDataMgr.queryBarcode(uri.getLastPathSegment());
            }
        });
        mControllerMap.put(3, new IRSController() {
            @Override
            public Cursor query(Uri uri) {
                MatrixCursor cs = new MatrixCursor(new String[]{"display", "value"});
                cs.addRow(new Object[]{MyBarcode.Category.TOTAL.displayString(), MyBarcode.Category.TOTAL.value()});
                cs.addRow(new Object[]{MyBarcode.Category.MEMBERSHIP.displayString(), MyBarcode.Category.MEMBERSHIP.value()});
                cs.addRow(new Object[]{MyBarcode.Category.COUPON.displayString(), MyBarcode.Category.COUPON.value()});
                cs.addRow(new Object[]{MyBarcode.Category.TEMPORARY.displayString(), MyBarcode.Category.TEMPORARY.value()});
                return cs;
            }
        });
        mControllerMap.put(4, new IRSController() {
            @Override
            public Cursor query(Uri uri) {
                return mDataMgr.queryBarcodesByCategory(uri.getPathSegments().get(1).toString());
            }
        });

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        LOG.i(TAG, "query : " + uri + " , where : " + selection + ", match : " + mUriMatcher.match(uri));
        Cursor cs = mControllerMap.get(mUriMatcher.match(uri)).query(uri);
        cs.setNotificationUri(this.getContext().getContentResolver(), AzAppConstants.URI.BARCODE_LIST);
        return cs;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


}

