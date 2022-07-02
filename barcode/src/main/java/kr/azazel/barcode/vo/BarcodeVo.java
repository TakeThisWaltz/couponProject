package kr.azazel.barcode.vo;

import androidx.annotation.Nullable;

public class BarcodeVo {
    private String rawValue;
    private int format;

//    public BarcodeVo(@NonNull BarcodeSource barcodeSource) {
//        super(barcodeSource);
//    }

    @Nullable
    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    public int getFormat() {
        return format;
    }

    public void setFormat(int format) {
        this.format = format;
    }
}
