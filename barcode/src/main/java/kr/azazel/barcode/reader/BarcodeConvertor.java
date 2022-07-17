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
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.QRCode;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import kr.azazel.barcode.vo.BarcodeVo;


/**
 * Created by ji on 2016. 10. 12..
 */

public class BarcodeConvertor {
    private static final String TAG = "BarcodeConvertor";

    public static BarcodeVo decodeWithZxing(Bitmap bitmap) {
        MultiFormatReader multiFormatReader = new MultiFormatReader();
        try {
            Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
            hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
            multiFormatReader.setHints(hints);


            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);

            if (source != null) {
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result rawResult = multiFormatReader.decode(binaryBitmap);

                return convertZXingToGoogleType(rawResult.getText(), rawResult.getBarcodeFormat().toString());
            }
        } catch (Exception re) {
            LOG.e(TAG, "decodeWithZxing err", re);
        } finally {
            multiFormatReader.reset();
        }
        return null;
    }

    public static void detectBarcodeByGoogle(Context context, Uri uri, Consumer<BarcodeVo> consumer) {
        LOG.d(TAG, "detectBarcode : " + uri + ", space : " + MemoryUtil.getPercentageMemoryFree(context));

        Bitmap bitmap = null;
        BarcodeScanner detector = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

            detector = BarcodeScanning.getClient();

            //detector.process(InputImage.fromBitmap(bitmap, 0))
            detector.process(InputImage.fromFilePath(context, uri))
                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        public void onSuccess(List<Barcode> barcodes) {
                            BarcodeVo barcode = null;
                            if (barcodes != null && barcodes.size() > 0) {
                                Barcode result = barcodes.get(0);
                                barcode = new BarcodeVo();
                                barcode.setRawValue(result.getRawValue());
                                barcode.setFormat(result.getFormat());
                                LOG.d(TAG, "detectBarcode - found : " + result.getRawValue() + ", type : " + result.getFormat());
                            } else
                                LOG.e(TAG, "No barcode is found... : " + barcodes);

                            consumer.accept(barcode);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            // ...
                            LOG.e(TAG, "detectBarcodeByGoogle err", e);
                            consumer.accept(null);
                        }
                    });

        } catch (IOException e) {
            LOG.e(TAG, "detectBarcode err", e);
        } finally {
//            if(detector != null) detector.release();
            if (bitmap != null) bitmap.recycle();
        }
    }
//
//    public static void detectBarcodeByGoogle(Context context, Uri uri) {
//        LOG.d(TAG, "detectBarcode : " + uri + ", space : " + MemoryUtil.getPercentageMemoryFree(context));
//
//        Bitmap bitmap = null;
//        try {
//            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
//
//
//            FirebaseVision.getInstance()
//                    .getVisionBarcodeDetector()
//                    .detectInImage(FirebaseVisionImage.fromBitmap(bitmap))
//                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
//                        @Override
//                        public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
//                            if (barcodes != null && barcodes.size() > 0) {
//                                FirebaseVisionBarcode result = barcodes.get(0);
//                                LOG.d(TAG, "detectBarcode - found : " + result.getRawValue() + ", type : " + result.getFormat());
//
//                            } else
//                                LOG.e(TAG, "No barcode is found... : " + barcodes);
//
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            // Task failed with an exception
//                            // ...
//                            LOG.e(TAG, "detectBarcodeByGoogle err", e);
//                        }
//                    });
//
//        } catch (IOException e) {
//            LOG.e(TAG, "detectBarcode err", e);
//        } finally {
////            if(detector != null) detector.release();
//            if (bitmap != null) bitmap.recycle();
//        }
//    }


    public static BarcodeVo detectBarcode(Context context, Uri uri) {
        LOG.d(TAG, "detectBarcode : " + uri + ", space : " + MemoryUtil.getPercentageMemoryFree(context));

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

            return decodeWithZxing(bitmap);

        } catch (IOException e) {
            LOG.e(TAG, "detectBarcode err", e);
        } finally {
//            if(detector != null) detector.release();
            if (bitmap != null) bitmap.recycle();
        }
        return null;
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

    public static BarcodeVo convertZXingToGoogleType(String value, String format) {
        BarcodeVo barcode = new BarcodeVo();
        barcode.setRawValue(value);
        switch (BarcodeFormat.valueOf(format)) {
            case CODABAR:
                barcode.setFormat(Barcode.FORMAT_CODABAR);
                break;
            case CODE_128:
                barcode.setFormat(Barcode.FORMAT_CODE_128);
                break;
            case CODE_39:
                barcode.setFormat(Barcode.FORMAT_CODE_39);
                break;
            case CODE_93:
                barcode.setFormat(Barcode.FORMAT_CODE_93);
                break;
            case EAN_13:
                barcode.setFormat(Barcode.FORMAT_EAN_13);
                break;
            case EAN_8:
                barcode.setFormat(Barcode.FORMAT_EAN_8);
                break;
            case ITF:
                barcode.setFormat(Barcode.FORMAT_ITF);
                break;
            case QR_CODE:
                barcode.setFormat(Barcode.FORMAT_QR_CODE);
                break;
            case UPC_A:
                barcode.setFormat(Barcode.FORMAT_UPC_A);
                break;
            case UPC_E:
                barcode.setFormat(Barcode.FORMAT_UPC_E);
                break;
            case PDF_417:
                barcode.setFormat(Barcode.FORMAT_PDF417);
                break;
            case AZTEC:
                barcode.setFormat(Barcode.FORMAT_AZTEC);
                break;
            //default 128?
            default:
                barcode.setFormat(Barcode.FORMAT_CODE_128);
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

    private static Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int img_width, int img_height) throws WriterException {
        if (contents == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        if (format == QR_CODE) {
            hints.put(EncodeHintType.QR_VERSION, 2);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        }
        String encoding = guessAppropriateEncoding(contents);
        if (encoding != null) {
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        //MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
//            if (format == QR_CODE) {
////                QRCodeWriter writer = new QRCodeWriter();
//                QRCode code = Encoder.encode(contents, ErrorCorrectionLevel.L, hints);
//                int quietZone = 4;
//                result = renderResult(code, img_width, img_height, quietZone);
//            } else {

            result = new MultiFormatWriter().encode(contents, format, img_width, img_height, hints);
//            }
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            LOG.e(TAG,"barcode encode err", iae);
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

    private static BitMatrix renderResult(QRCode code, int width, int height, int quietZone) {
        ByteMatrix input = code.getMatrix();
        if (input == null) {
            throw new IllegalStateException();
        }
        int inputWidth = input.getWidth();
        int inputHeight = input.getHeight();
        int qrWidth = inputWidth + (quietZone * 2);
        int qrHeight = inputHeight + (quietZone * 2);
        int outputWidth = Math.max(width, qrWidth);
        int outputHeight = Math.max(height, qrHeight);

        int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
        // Padding includes both the quiet zone and the extra white pixels to accommodate the requested
        // dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
        // If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
        // handle all the padding from 100x100 (the actual QR) up to 200x160.
        int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
        int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

        BitMatrix output = new BitMatrix(outputWidth, outputHeight);

        for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
            // Write the contents of this row of the barcode
            for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
                if (input.get(inputX, inputY) == 1) {
                    output.setRegion(outputX, outputY, multiple, multiple);
                }
            }
        }

        return output;
    }
}
