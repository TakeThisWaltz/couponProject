package kr.azazel.barcode.vo;

public enum BarcodeSort {
    REG_ASC("등록순", "crt_dt asc"),
    REG_DESC("최신순", "crt_dt desc"),
    //HIT("자주쓰는순", "crt_dt asc"),
    EXPIRE_DATE("유효기간순", "expiration_dt asc"),
    ;

    public String displayName;
    public String query;

    BarcodeSort(String name, String query) {
        this.displayName = name;
        this.query = query;
    }

    public String toString() {
        return displayName;
    }
}
