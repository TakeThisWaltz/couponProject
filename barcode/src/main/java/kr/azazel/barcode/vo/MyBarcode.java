package kr.azazel.barcode.vo;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;

import com.azazel.framework.AzApplication;
import com.azazel.framework.util.FileTool;
import com.azazel.framework.util.LOG;

import java.io.File;

import kr.azazel.barcode.AzAppConstants;
import kr.azazel.barcode.FileUtil;
import kr.azazel.barcode.R;
import kr.azazel.barcode.local.AzAppDataHelper;
import kr.azazel.barcode.reader.BarcodeConvertor;

/**
 * Created by ji on 2016. 10. 12..
 */
public class MyBarcode {
    private static final String TAG = "MyBarcode";

    public int id = -1;

    public int category;

    public String code;
    public int type;
    public String originImage;
    public String barcodeImage;
    public String coverImage;
    public long expirationDate;

    public String title;
    public String description;
    public String brand;
    public int brandType;
    public String iconUrl;

    public long crtDt;
    public long mdfyDt;

    public MyBarcode(int category, String code, int type, long expirationDate, String title, String description, String brand, int brandType) {
        this.category = category;
        this.code = code;
        this.type = type;
        this.expirationDate = expirationDate;
        this.title = title;
        this.description = description;
        this.brand = brand;
        this.brandType = brandType;
    }

    public MyBarcode(Cursor cs) {
        this.id = cs.getInt(cs.getColumnIndex("_id"));
        this.category = cs.getInt(cs.getColumnIndex("category"));
        this.code = cs.getString(cs.getColumnIndex("code"));
        this.type = cs.getInt(cs.getColumnIndex("type"));
        this.originImage = cs.getString(cs.getColumnIndex("origin_image"));
        this.barcodeImage = cs.getString(cs.getColumnIndex("barcode_image"));
        this.coverImage = cs.getString(cs.getColumnIndex("cover_image"));
        this.expirationDate = cs.getLong(cs.getColumnIndex("expiration_dt"));
        this.title = cs.getString(cs.getColumnIndex("title"));
        this.description = cs.getString(cs.getColumnIndex("description"));
        this.brand = cs.getString(cs.getColumnIndex("brand"));
        this.brandType = cs.getInt(cs.getColumnIndex("brand_type"));
        this.iconUrl = cs.getString(cs.getColumnIndex("icon_url"));
        this.crtDt = cs.getLong(cs.getColumnIndex("crt_dt"));
        this.mdfyDt = cs.getLong(cs.getColumnIndex("mdfy_dt"));
    }

    @Override
    public String toString() {
        return "MyBarcode : id : " + id + ", title : " + title + ", code : " + code + ", type : " + type + ", img : " + barcodeImage + ", cover : " + coverImage +", origin : " + originImage;
    }

    public static boolean saveBarcode(int category, String code, String title, int type, String desc, String brand, Uri org, Bitmap imgCode, Bitmap imgCover, long expirationDate) {
        MyBarcode barcode = new MyBarcode(category, code, type, expirationDate, title, desc, brand, 0);

        int id = AzAppDataHelper.getInstance().insertBarcode(barcode);

        String pathCode = AzApplication.APP_CONTEXT.getFilesDir() + "/barcode_" + id + "_" + System.currentTimeMillis();
        String pathOrigin = "";

        if (org != null) {
            pathOrigin = AzApplication.APP_CONTEXT.getFilesDir() + "/barcode_origin_" + id + "_" + System.currentTimeMillis();
            FileUtil.copy(AzApplication.APP_CONTEXT, org, new File(pathOrigin));
            barcode.originImage = pathOrigin;
        }

        boolean savedCode = BarcodeConvertor.saveBitmaptoJpeg(imgCode, pathCode);
        String pathCover = "";
        boolean savedCover = true;
        if (imgCover != null) {
            pathCover = AzApplication.APP_CONTEXT.getFilesDir() + "/barcode_cover_" + id + "_" + System.currentTimeMillis();
            savedCover = BarcodeConvertor.saveBitmaptoJpeg(imgCover, pathCover);
        }

        if (savedCode && savedCover)
            AzAppDataHelper.getInstance().updateImagePath(id, pathOrigin, pathCode, pathCover);
        else {
            AzAppDataHelper.getInstance().deleteBarcode(id);

            FileTool.deleteFile(pathCode);
            if (imgCover != null) FileTool.deleteFile(pathCover);

        }

        AzApplication.APP_CONTEXT.getContentResolver().notifyChange(AzAppConstants.URI.BARCODE_LIST, null);
        return savedCode && savedCover;
    }

    public void update(Bitmap imgCover) {
        LOG.d(TAG, "updateCoverImg - " + this);
        if (id > -1) {
            if (imgCover != null) {
                String path = AzApplication.APP_CONTEXT.getFilesDir() + "/barcode_cover_" + id + "_" + System.currentTimeMillis();
                boolean savedCover = BarcodeConvertor.saveBitmaptoJpeg(imgCover, path);
                if (savedCover) {
                    FileTool.deleteFile(coverImage);
                    coverImage = path;
                } else {
                    FileTool.deleteFile(path);
                }
            }

            AzAppDataHelper.getInstance().updateBarcode(this);
            AzApplication.APP_CONTEXT.getContentResolver().notifyChange(AzAppConstants.URI.BARCODE_LIST, null);
        }
    }

    public void delete() {
        FileTool.deleteFile(barcodeImage);
        FileTool.deleteFile(coverImage);

        AzAppDataHelper.getInstance().deleteBarcode(id);
        AzApplication.APP_CONTEXT.getContentResolver().notifyChange(AzAppConstants.URI.BARCODE_LIST, null);
    }


    public static enum Category {

        TOTAL(R.string.code_type_tot, 0),
        MEMBERSHIP(R.string.code_type_mem, 1),
        COUPON(R.string.code_type_cou, 2),
        TEMPORARY(R.string.code_type_etc, 3);

        private String displayString;
        private int value;

        private Category(int resId, int value) {
            this.displayString = AzApplication.APP_CONTEXT.getString(resId);
            this.value = value;
        }

        @Override
        public String toString() {
            return displayString;
        }

        public String displayString() {
            return displayString;
        }

        public int value() {
            return value;
        }

        public static Category fromDisplayString(String displayString) {

            if (displayString != null) {
                for (Category flavour : Category.values()) {
                    if (displayString.equalsIgnoreCase(flavour.displayString)) {
                        return flavour;
                    }
                }
            }

            throw new IllegalArgumentException("No Flavour with display string " + displayString + " found");

        }

        public static Category fromValue(int value) {
            for (Category flavour : Category.values()) {
                if (value == flavour.value) {
                    return flavour;
                }
            }

            throw new IllegalArgumentException("No Category with value " + value + " found");
        }

    }
}
