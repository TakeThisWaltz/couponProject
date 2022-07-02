package kr.azazel.barcode;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;

import android.text.TextUtils;
import android.view.View;

import androidx.core.content.FileProvider;

import com.azazel.framework.util.FileTool;
import com.azazel.framework.util.LOG;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import kr.azazel.barcode.reader.BarcodeConvertor;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static String getFilePathFromURI(Context context, Uri contentUri) {
        String fileName = getFileName(contentUri);
        if (!TextUtils.isEmpty(fileName)) {
            File copyFile = new File(context.getFilesDir() + File.separator + fileName);
            copy(context, contentUri, copyFile);
            return copyFile.getAbsolutePath();
        }
        return null;
    }

    public static String getFileName(Uri uri) {
        if (uri == null) return null;
        String fileName = null;
        String path = uri.getPath();
        int cut = path.lastIndexOf('/');
        if (cut != -1) {
            fileName = path.substring(cut + 1);
        }
        return fileName;
    }

    public static void copy(Context context, Uri srcUri, File dstFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(dstFile);
            IOUtils.copy(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            LOG.e(TAG, "copy err", e);
        }
    }

    public static void shareBitmapImage(View view, Bitmap bitmap) {

        File cachePath = new File(view.getContext().getCacheDir(), "images");
        cachePath.mkdirs(); // don't forget to make the directory
        boolean saved = BarcodeConvertor.saveBitmaptoJpeg(bitmap, cachePath + "/image.png");
        File newFile = new File(cachePath + "/image.png");
        Uri contentUri = FileProvider.getUriForFile(view.getContext(), "kr.azazel.barcode.fileprovider", newFile);

        if (contentUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, view.getContext().getContentResolver().getType(contentUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TITLE, "[픽미픽미]\n" + "article.title" + "\n");
            view.getContext().startActivity(Intent.createChooser(shareIntent, "Choose an app"));
        }
    }

    public static void shareLocalFile(View view, String filePath) {
        if (filePath == null) return;


        try {
            File cachePath = new File(view.getContext().getCacheDir(), "images");
            cachePath.mkdirs(); // don't forget to make the directory
            FileTool.writeToFile(filePath, 0, cachePath + "/image.png", null);

            File newFile = new File(cachePath + "/image.png");
            Uri contentUri = FileProvider.getUriForFile(view.getContext(), "kr.azazel.barcode.fileprovider", newFile);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                shareIntent.setDataAndType(contentUri, view.getContext().getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_TITLE, "[픽미픽미]\n" + "article.title" + "\n");
                view.getContext().startActivity(Intent.createChooser(shareIntent, "Choose an app"));
            }
        } catch (IOException e) {
            LOG.e(TAG, "shareLocalFile err", e);
        }
    }


    public static Bitmap combineImagesVertical(Bitmap c, Bitmap s) {
        Bitmap cs = null;

        int width = Math.max(c.getWidth(), s.getWidth());
        int height = c.getHeight() + s.getHeight();

        cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas comboImage = new Canvas(cs);

        comboImage.drawBitmap(c, 0f, 0f, null);
        comboImage.drawBitmap(s, 0f, c.getHeight(), null);

        return cs;
    }
}
