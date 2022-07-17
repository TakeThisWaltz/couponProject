package kr.azazel.barcode.vo;

import com.azazel.framework.AzApplication;

import kr.azazel.barcode.R;

public enum BarcodeSort {
    REG_ASC(R.string.order_type_reg_asc, "crt_dt asc"),
    REG_DESC(R.string.order_type_reg_desc, "crt_dt desc"),
    HIT(R.string.order_type_hits, "hits desc, title asc"),
    NAME(R.string.order_type_name, "title asc"),
    EXPIRE_DATE(R.string.order_type_expire, "expiration_dt asc"),
    ;

    public int resId;
    public String displayName;
    public String query;

    BarcodeSort(int resId, String query) {
        this.resId = resId;
        this.displayName = AzApplication.APP_CONTEXT.getString(resId);
        this.query = query;
    }

    public String toString() {
        return displayName;
    }
}
