package kr.azazel.barcode.reader;

import static com.google.zxing.BarcodeFormat.AZTEC;
import static com.google.zxing.BarcodeFormat.CODABAR;
import static com.google.zxing.BarcodeFormat.CODE_128;
import static com.google.zxing.BarcodeFormat.CODE_39;
import static com.google.zxing.BarcodeFormat.CODE_93;
import static com.google.zxing.BarcodeFormat.EAN_13;
import static com.google.zxing.BarcodeFormat.EAN_8;
import static com.google.zxing.BarcodeFormat.ITF;
import static com.google.zxing.BarcodeFormat.PDF_417;
import static com.google.zxing.BarcodeFormat.QR_CODE;
import static com.google.zxing.BarcodeFormat.UPC_A;
import static com.google.zxing.BarcodeFormat.UPC_E;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.azazel.framework.util.LOG;
import com.azazel.framework.util.MemoryUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * Created by ji on 2016. 10. 12..
 */

public class BarcodeConvertor {
    private static final String TAG = "BarcodeConvertor";

    public static void detectBarcode(Context context, Uri uri, final Consumer<Barcode> handler) {
        LOG.d(TAG, "detectBarcode : " + uri + ", space : " + MemoryUtil.getPercentageMemoryFree(context));

        Bitmap bitmap = null;
        BarcodeScanner detector = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

            detector = BarcodeScanning.getClient();

            detector.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                @Override
                public void onSuccess(List<Barcode> barcodes) {
                    if (barcodes != null && barcodes.size() > 0) {
                        Barcode result = barcodes.get(0);
                        LOG.d(TAG, "detectBarcode - found : " + result.getRawValue() + ", type : " + result.getFormat());
                        handler.accept(result);
                    } else
                        LOG.e(TAG, "No barcode is found... : " + barcodes);

                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            // ...
                        }
                    });

        } catch (IOException e) {
            LOG.e(TAG, "detectBarcode err", e);
        } finally {
//            if(detector != null) detector.release();
            if (bitmap != null) bitmap.recycle();
        }
    }

    public static boolean saveBitmaptoJpeg(Bitmap bitmap, String path) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return true;
        } catch (FileNotFoundException exception) {
            LOG.e("FileNotFoundException", exception.getMessage());
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException e) {

            }
        }
        return false;
    }

    public static Bitmap getBitmap(String barcode, int barcodeType, int width, int height) {
        Bitmap barcodeBitmap = null;
        BarcodeFormat barcodeFormat = convertToZXingFormat(barcodeType);
        try {
            barcodeBitmap = encodeAsBitmap(barcode, barcodeFormat, width, height);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return barcodeBitmap;
    }

    private static BarcodeFormat convertToZXingFormat(int format) {
        switch (format) {
            case Barcode.FORMAT_CODABAR:
                return CODABAR;
            case Barcode.FORMAT_CODE_128:
                return CODE_128;
            case Barcode.FORMAT_CODE_39:
                return CODE_39;
            case Barcode.FORMAT_CODE_93:
                return CODE_93;
            case Barcode.FORMAT_EAN_13:
                return EAN_13;
            case Barcode.FORMAT_EAN_8:
                return EAN_8;
            case Barcode.FORMAT_ITF:
                return ITF;
            case Barcode.FORMAT_QR_CODE:
                return QR_CODE;
            case Barcode.FORMAT_UPC_A:
                return UPC_A;
            case Barcode.FORMAT_UPC_E:
                return UPC_E;
            case Barcode.FORMAT_PDF417:
                return PDF_417;
            case Barcode.FORMAT_AZTEC:
                return AZTEC;
            //default 128?
            default:
                return CODE_128;
        }
    }

//    public static Barcode convertZXingToGoogleType(String value, String format){
//        Barcode barcode = new Barcode();
//        barcode.getRawValue() = value;
//        switch (valueOf(format)){
//            case CODABAR:
//                barcode = Barcode.FORMAT_CODABAR;
//                break;
//            case CODE_128:
//                barcode.format = Barcode.FORMAT_CODE_128;
//                break;
//            case CODE_39:
//                barcode.format = Barcode.FORMAT_CODE_39;
//                break;
//            case CODE_93:
//                barcode.format = Barcode.FORMAT_CODE_93;
//                break;
//            case EAN_13:
//                barcode.format = Barcode.FORMAT_EAN_13;
//                break;
//            case EAN_8:
//                barcode.format = Barcode.FORMAT_EAN_8;
//                break;
//            case ITF:
//                barcode.format = Barcode.FORMAT_ITF;
//                break;
//            case QR_CODE:
//                barcode.format = Barcode.FORMAT_QR_CODE;
//                break;
//            case UPC_A:
//                barcode.format = Barcode.FORMAT_UPC_A;
//                break;
//            case UPC_E:
//                barcode.format = Barcode.FORMAT_UPC_E;
//                break;
//            case PDF_417:
//                barcode.format = Barcode.FORMAT_PDF417;
//                break;
//            case AZTEC:
//                barcode.format = Barcode.FORMAT_AZTEC;
//                break;
//            //default 128?
//            default:
//                barcode.format = Barcode.FORMAT_CODE_128;
//                break;
//        }
//        return barcode;
//    }


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

    private static Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int img_width, int img_height) throws WriterException {
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
