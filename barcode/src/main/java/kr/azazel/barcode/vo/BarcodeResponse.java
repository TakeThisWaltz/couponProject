package kr.azazel.barcode.vo;

public class BarcodeResponse {
    private String store;
    private String item;
    private String expireDate;
    private String type;

    public String getStore() {
        if(store == null) return "";
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public String getItem() {
        if(item == null) return "";
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MyBarcode.Category getCategory() {
        if (this.type == null) return MyBarcode.Category.TEMPORARY;

        switch (this.type) {
            case "MEMBERSHIP":
                return MyBarcode.Category.MEMBERSHIP;
            case "COUPON":
                return MyBarcode.Category.COUPON;
            default:
                return MyBarcode.Category.TEMPORARY;
        }
    }
}
