package kr.azazel.barcode;

import android.net.Uri;
import android.os.Environment;

import com.azazel.framework.AzApplication;

public class AzAppConstants {

    public static final String BASE_PATH = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/" + AzApplication.APP_CONFIG.getName();

    public final static int MAIN_SERVICE = 3067;
    public static final int[] SERVICE_LIST = new int[]{MAIN_SERVICE};

    public final static int BARCODE_IMG_WIDTH = 1350;
    public final static int BARCODE_IMG_HEIGHT = 600;

    public interface URI{
        public static final String AHTHORITY = "kr.azazel.barcode.provider";
        public static final Uri BASE_URI = Uri.parse("content://" + AHTHORITY);
        public static final Uri BARCODE_LIST = Uri.withAppendedPath(BASE_URI, "barcodes");
        public static final Uri CATEGORY_LIST = Uri.withAppendedPath(BASE_URI, "categories");
    }
    public interface Event{
        public static final int EXIT = 1;
        public static final int INIT_START = 2;
        public static final int REQUEST_LOGIN = 3;
        public static final int LOGIN_COMPLETED = 4;
        public static final int LOGIN_FAILED = 5;
        public static final int SCRAP_CHANGED = 6;
        public static final int MINE_LOADED = 7;
        public static final int TRY_EXIT = 8;
        public static final int TIME_OUT = 9;
        public static final int SERVICE_AVAILABLE = 10;
        public static final int SERVICE_NOT_AVAILABLE = 11;
        public static final int SERVICE_CONNECTING_START = 12;
        public static final int SERVICE_CONNECTING_END = 13;

        public static final int LISTVIEW_SCROLL_START = 21;
        public static final int LISTVIEW_SCROLL_END = 22;
    }


    
    interface Intents {
        public static final String START_APP = "START_APP";
        public static final String NETWORK_CHAKGED = "NETWORK_CHAKGED";
    }

    interface Keys {
        public static final String LIST_POSITION = "LIST_POSITION";
    }
}
