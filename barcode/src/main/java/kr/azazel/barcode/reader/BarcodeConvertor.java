package kr.azazel.barcode.reader;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.widget.Toast;

import com.azazel.framework.util.LOG;
import com.azazel.framework.util.MemoryUtil;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import kr.azazel.barcode.R;

import static com.google.zxing.BarcodeFormat.*;

import androidx.appcompat.app.AlertDialog;


/**
 * Created by ji on 2016. 10. 12..
 */

public class BarcodeConvertor {
    private static final String TAG = "BarcodeConvertor";

    public static Barcode detectBarcode(Context context, Uri uri){
        LOG.d(TAG, "detectBarcode : " + uri + ", space : " + MemoryUtil.getPercentageMemoryFree(context));

        Barcode result = null;
        Bitmap bitmap = null;
        BarcodeDetector detector = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

            detector = new BarcodeDetector.Builder(context.getApplicationContext())
                            .setBarcodeFormats(Barcode.ALL_FORMATS)
                            .build();
            if(detector.isOperational()) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<Barcode> barcodes = detector.detect(frame);
                if (barcodes != null && barcodes.size() > 0) {
                    result = barcodes.valueAt(0);
                    LOG.d(TAG, "detectBarcode - found : " + result.rawValue + ", type : " + result.format);
                } else
                    LOG.e(TAG, "No barcode is found... : " + barcodes);
            }else{
                IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                boolean hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null;
                float availableSpace = MemoryUtil.getPercentageMemoryFree(context);
                LOG.d(TAG, "BarcodeDetector is not operational... dependency download is in progress.. hasLowStorage : " + hasLowStorage + ", memory : " + availableSpace);

                if (hasLowStorage || availableSpace < 0.1f) {
                    if(context instanceof Activity){
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("기기 용량이 부족합니다.")        // 제목 설정
                                .setMessage("저장공간이 부족하여 바코드 분석에 필요한 안드로이드 업데이트 설치에 실패했습니다." +
                                        "\n업데이트를 설치하려면 10%이상의 여유공간이 필요합니다.\n(현재 여유 공간 : "
                                        + (Math.round(availableSpace * 100f)) + "%)\n해당 공간은 설치에 다 쓰이지 않으며 최초 동작의 환경입니다.")
                                .setCancelable(true)        // 뒤로 버튼 클릭시 취소 가능 설정
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                                    // 확인 버튼 클릭시 설정
                                    public void onClick(DialogInterface dialog, int whichButton){

                                    }
                                });


                        AlertDialog dialog = builder.create();    // 알림창 객체 생성
                        dialog.show();
                        return null;
                    }else {
                        Toast.makeText(context, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                        LOG.d(TAG, context.getString(R.string.low_storage_error));
                    }
                }
            }
//            for(int i=0;i<barcodes.size();i++){
//                Barcode code = barcodes.valueAt(i);
//                Rect corner = code.getBoundingBox();
//                Log.d("CODE", "key :  " + barcodes.keyAt(i) + ", value : " + code.displayValue + ", " + code.rawValue + ", " + code.valueFormat + ", bound : " + corner.flattenToString());
//                Bitmap cropped = Bitmap.createBitmap(bitmap, corner.left, corner.top, corner.width(), corner.height());
//                saveBitmaptoJpeg(cropped, "Azazel", "cropped_" + barcodes.keyAt(i));
//
//            }
        } catch (IOException e) {
            LOG.e(TAG, "detectBarcode err", e);
        } finally {
            if(detector != null) detector.release();
            if(bitmap != null) bitmap.recycle();
        }
        return result;
    }

    public static boolean saveBitmaptoJpeg(Bitmap bitmap, String path){
        FileOutputStream out = null;
        try{
            out = new FileOutputStream(path);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return true;
        }catch(FileNotFoundException exception){
            LOG.e("FileNotFoundException", exception.getMessage());
        }finally {
            if(out!=null) try {
                out.close();
            } catch (IOException e) {

            }
        }
        return false;
    }

    public static Bitmap getBitmap(String barcode, int barcodeType, int width, int height)
    {
        Bitmap barcodeBitmap = null;
        BarcodeFormat barcodeFormat = convertToZXingFormat(barcodeType);
        try
        {
            barcodeBitmap = encodeAsBitmap(barcode, barcodeFormat, width, height);
        }
        catch (WriterException e)
        {
            e.printStackTrace();
        }
        return barcodeBitmap;
    }

    private static BarcodeFormat convertToZXingFormat(int format)
    {
        switch (format)
        {
            case Barcode.CODABAR:
                return CODABAR;
            case Barcode.CODE_128:
                return CODE_128;
            case Barcode.CODE_39:
                return CODE_39;
            case Barcode.CODE_93:
                return CODE_93;
            case Barcode.EAN_13:
                return EAN_13;
            case Barcode.EAN_8:
                return EAN_8;
            case Barcode.ITF:
                return ITF;
            case Barcode.QR_CODE:
                return QR_CODE;
            case Barcode.UPC_A:
                return UPC_A;
            case Barcode.UPC_E:
                return UPC_E;
            case Barcode.PDF417:
                return PDF_417;
            case Barcode.AZTEC:
                return AZTEC;
            //default 128?
            default:
                return CODE_128;
        }
    }

    public static Barcode convertZXingToGoogleType(String value, String format){
        Barcode barcode = new Barcode();
        barcode.rawValue = value;
        switch (valueOf(format)){
            case CODABAR:
                barcode.format = Barcode.CODABAR;
                break;
            case CODE_128:
                barcode.format = Barcode.CODE_128;
                break;
            case CODE_39:
                barcode.format = Barcode.CODE_39;
                break;
            case CODE_93:
                barcode.format = Barcode.CODE_93;
                break;
            case EAN_13:
                barcode.format = Barcode.EAN_13;
                break;
            case EAN_8:
                barcode.format = Barcode.EAN_8;
                break;
            case ITF:
                barcode.format = Barcode.ITF;
                break;
            case QR_CODE:
                barcode.format = Barcode.QR_CODE;
                break;
            case UPC_A:
                barcode.format = Barcode.UPC_A;
                break;
            case UPC_E:
                barcode.format = Barcode.UPC_E;
                break;
            case PDF_417:
                barcode.format = Barcode.PDF417;
                break;
            case AZTEC:
                barcode.format = Barcode.AZTEC;
                break;
            //default 128?
            default:
                barcode.format = Barcode.CODE_128;
                break;
        }
        return barcode;
    }


    /**************************************************************
     * getting from com.google.zxing.client.android.encode.QRCodeEncoder
     *
     * See the sites below
     * http://code.google.com/p/zxing/
     * http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/encode/EncodeActivity.java
     * http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/encode/QRCodeEncoder.java
     */

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private static Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int img_width, int img_height) throws WriterException
    {
        if (contents == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        String encoding = guessAppropriateEncoding(contents);
        if (encoding != null) {
            hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
            result = writer.encode(contents, format, img_width, img_height, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }
}
