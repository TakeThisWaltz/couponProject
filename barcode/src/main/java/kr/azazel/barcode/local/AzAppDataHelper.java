package kr.azazel.barcode.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.azazel.framework.AzApplication;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.LOG;

import kr.azazel.barcode.vo.MyBarcode;

public class AzAppDataHelper extends SQLiteOpenHelper {
    private static final String TAG = "AzAppDataHelper";

    public static final String TbBarcode = "tb_barcode";

    private static final String DB_NAME = "barcode_manager.db";

    private static final int VERSION = 2;

    private static AzAppDataHelper INSTANCE;

    private SQLiteDatabase mDb;

    public static synchronized AzAppDataHelper getInstance() {
        if (INSTANCE == null)
            INSTANCE = new AzAppDataHelper(AzApplication.APP_CONTEXT);

        return INSTANCE;
    }

    public static synchronized AzAppDataHelper getInstance(Context context) {
        if (INSTANCE == null)
            INSTANCE = new AzAppDataHelper(context);

        return INSTANCE;
    }

    public AzAppDataHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
        LOG.i(TAG, "AzAppDataHelper - context : " + context);
        mDb = getWritableDatabase();
    }

    public synchronized SQLiteDatabase getDatabase() {
        return mDb;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        LOG.i(TAG, "onCreate");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TbBarcode + " ("
                + "_id INTEGER PRIMARY KEY, "
                + "code TEXT, "
                + "type TEXT, "
                + "origin_image TEXT, "
                + "barcode_image TEXT, "
                + "cover_image TEXT, "
                + "expiration_dt INTEGER, "
                + "title TEXT, "
                + "description TEXT, "
                + "brand TEXT, "
                + "brand_type INTEGER, "
                + "icon_url INTEGER, "
                + "category INTEGER, "
                + "crt_dt INTEGER, "
                + "mdfy_dt INTEGER );");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOG.f(TAG, "onUpgrade - old : " + oldVersion + ", new : " + newVersion);
        if (oldVersion <= 1 && newVersion > 1) {
            db.execSQL("ALTER TABLE " + TbBarcode + " ADD COLUMN origin_image TEXT DEFAULT NULL");
        }
    }


    public int insertBarcode(MyBarcode code) {
        ContentValues cv = new ContentValues();
        cv.put("code", code.code);
        cv.put("type", code.type);
        cv.put("origin_image", code.originImage);
        cv.put("barcode_image", code.barcodeImage);
        cv.put("cover_image", code.coverImage);
        cv.put("expiration_dt", code.expirationDate);
        cv.put("title", code.title);
        cv.put("description", code.description);
        cv.put("brand", code.brand);
        cv.put("brand_type", code.brandType);
        cv.put("icon_url", code.iconUrl);
        cv.put("category", code.category);
        cv.put("crt_dt", System.currentTimeMillis());
        cv.put("mdfy_dt", System.currentTimeMillis());

        long id = mDb.insert(TbBarcode, null, cv);
        LOG.i(TAG, "insertBarcode - id : " + id + ", name : " + code.title);
        return (int) id;
    }

    public int updateBarcode(MyBarcode code) {
        ContentValues cv = new ContentValues();
        cv.put("code", code.code);
        cv.put("type", code.type);
        cv.put("origin_image", code.originImage);
        cv.put("barcode_image", code.barcodeImage);
        cv.put("cover_image", code.coverImage);
        cv.put("expiration_dt", code.expirationDate);
        cv.put("title", code.title);
        cv.put("description", code.description);
        cv.put("brand", code.brand);
        cv.put("brand_type", code.brandType);
        cv.put("icon_url", code.iconUrl);
        cv.put("category", code.category);
        cv.put("crt_dt", System.currentTimeMillis());
        cv.put("mdfy_dt", System.currentTimeMillis());

        long id = mDb.update(TbBarcode, cv, "_id = " + code.id, null);
        LOG.i(TAG, "updateBarcode - id : " + code.id + ", name : " + code.title);
        return (int) id;
    }
//
//    public void insertWall(Wall wall) {
//        ContentValues cv = new ContentValues();
//        cv.put("_id", wall.id);
//        cv.put("ch_id", wall.chId);
//        cv.put("name", wall.name);
//        cv.put("anonymous_flag", wall.isAnonymous ? 1 : 0);
//        cv.put("type", wall.type);
//        cv.put("crt_dt", wall.crtDt);
//        cv.put("mdfy_dt", wall.mdfyDt);
//
//        mDb.insert(TbWall, null, cv);
//        LOG.i(TAG, "insertWall - name : " + wall.name + ", id : " + wall.id);
//    }
//
//    public Wall getWall(int id) {
//        Cursor cs = mDb.query(TbWall, null, "_id = " + id, null, null, null, null);
//        Wall wall = null;
//        if (cs.moveToNext())
//            wall = new Wall(cs);
//        return wall;
//    }

    public void updateImagePath(int id, String pathOrigin, String pathCode, String pathCover) {
        Cursor cs = mDb.rawQuery("update " + TbBarcode + " set origin_image = ?, barcode_image = ?, cover_image = ? where _id = ?", new String[]{pathOrigin, pathCode, pathCover, id + ""});
        AzUtil.printCursor(TAG, cs, false);
    }

    public Cursor queryBarcodes() {
        return mDb.query(TbBarcode, null, null, null, null, null, null);
    }

    public Cursor queryBarcodesByCategory(String category) {
        LOG.d(TAG, "queryBarcodesByCategory : " + category);
        return mDb.query(TbBarcode, null, "0".equals(category) ? null : "category = " + category, null, null, null, null);
    }

    public Cursor queryBarcodesByCategory(String category, String keyword, String sort) {
        String query = "select * from tb_barcode where " +
                "1=1" +
                ("0".equals(category) ? "" : " and category = " + category) +
                (TextUtils.isEmpty(keyword) || "null".equals(keyword) ? "" : " and (title like '%" + keyword.replaceAll("\\'", "") + "%' or description like '%" + keyword.replaceAll("\\'", "") + "%')") +
                " order by " + sort;
        LOG.d(TAG, "queryBarcodesByCategory : " + query);
        return mDb.rawQuery(query, null);
//        return mDb.query(TbBarcode, null,
//                "0".equals(category) ? null : "category = " + category +
//                        (TextUtils.isEmpty(keyword)?"":" and title like '%"+keyword+"%'"),
//                null, null, null, null);
    }

    public Cursor queryBarcode(String id) {
        return mDb.query(TbBarcode, null, "_id = " + id, null, null, null, null);
    }


    public void deleteBarcode(int id) {
        int deleted = mDb.delete(TbBarcode, "_id = " + id, null);

        LOG.i(TAG, "deleteBarcode - deleted : " + deleted);
    }
}
