package kr.azazel.barcode.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.azazel.framework.AzApplication;
import com.azazel.framework.AzSimpleWorker;
import com.azazel.framework.util.LOG;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import kr.azazel.barcode.AzAppConstants;
import kr.azazel.barcode.R;
import kr.azazel.barcode.local.AzAppDataHelper;
import kr.azazel.barcode.vo.MyBarcode;

public class BackupRestoreUtil {
    private static final String TAG = "BackupRestoreUtil";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void backupFile(Context context) {
        File cachePath = new File(context.getCacheDir(), "backup");
        cachePath.mkdirs();

        File zipFile = new File(cachePath + "/pickme_backup.pkm");
        BackupRestoreUtil.makeZipFile(zipFile);

        Uri contentUri = FileProvider.getUriForFile(context, "kr.azazel.barcode.fileprovider", zipFile);

        sendEmail(context, contentUri);
    }

    private static void sendEmail(Context context, Uri contentUri) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
// set the type to 'email'
        emailIntent.setType("vnd.android.cursor.dir/email");
//        String to[] = {"asd@gmail.com"};
//        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
// the attachment
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file

        emailIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
// the mail subject
//        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
        context.startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }
//    private static void sendEmail(Context context, Uri contentUri) {
//        Intent shareIntent = new Intent();
//        shareIntent.setAction(Intent.ACTION_SEND);
//        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
//        shareIntent.setDataAndType(contentUri, "application/octet-stream");
//        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
//        shareIntent.putExtra(Intent.EXTRA_TITLE, "[픽미픽미]\n" + "article.title" + "\n");
//        context.startActivity(Intent.createChooser(shareIntent, "Choose an app"));
//    }

    public static File makeZipFile(File file) {
        try {
            if (file.exists()) {
                file.delete();
            }

            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                List<MyBarcode> codes = AzAppDataHelper.getInstance().getAllBarcodeSummary();

                zos.putNextEntry(new ZipEntry("barcode_summary"));
                zos.write(OBJECT_MAPPER.convertValue(codes, JsonNode.class)
                        .toString().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                Cursor cursor = AzAppDataHelper.getInstance().queryBarcodes();
                while (cursor.moveToNext()) {
                    MyBarcode barcode = new MyBarcode(cursor);
                    barcode.originImage = clearDevicePath(barcode.originImage);
                    barcode.coverImage = clearDevicePath(barcode.coverImage);
                    barcode.barcodeImage = clearDevicePath(barcode.barcodeImage);

                    JsonNode json = OBJECT_MAPPER.convertValue(barcode, JsonNode.class);
                    zos.putNextEntry(new ZipEntry("barcode_json_" + barcode.id));
                    zos.write(json.toString().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();

                    saveFile(zos, barcode.originImage);
                    saveFile(zos, barcode.coverImage);
                    saveFile(zos, barcode.barcodeImage);
                }
            }

            return file;
        } catch (Exception e) {
            LOG.e(TAG, "makeZipFile err", e);
        }
        return null;
    }

    private static String clearDevicePath(String orgPath) {
        return orgPath == null ? null :
                orgPath.replace(AzApplication.APP_CONTEXT.getFilesDir().getAbsolutePath(), "");
    }

    private static String restoreDevicePath(String orgPath, int orgId, int newId) {
        return orgPath == null ? null :
                AzApplication.APP_CONTEXT.getFilesDir().getAbsolutePath() + orgPath.replace("_" + orgId + "_", "_" + newId + "_");
    }

    private static void saveFile(ZipOutputStream zos, String filePath) throws IOException {
        if (TextUtils.isEmpty(filePath)) return;

        File file = new File(AzApplication.APP_CONTEXT.getFilesDir().getAbsolutePath() + filePath);
        if (file.exists()) {
            zos.putNextEntry(new ZipEntry(filePath.substring(1)));
            try (FileInputStream fis = new FileInputStream(file)) {
                IOUtils.copy(fis, zos);
            }
            zos.closeEntry();
        }
    }

    public static void confirmRestore(Activity activity, Uri uri) {
        AzAppDataHelper dataHelper = AzAppDataHelper.getInstance();

        try (ZipInputStream zis = new ZipInputStream(AzApplication.APP_CONTEXT.getContentResolver().openInputStream(uri))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry.getName().equals("barcode_summary")) {
                String codes = IOUtils.toString(zis, StandardCharsets.UTF_8);
                if (!TextUtils.isEmpty(codes)) {

                    List<MyBarcode> fileCodes = OBJECT_MAPPER.readValue(codes, new TypeReference<>() {
                    });

                    List<MyBarcode> dbCodes = dataHelper.getAllBarcodeSummary();

                    int fileCount = fileCodes.size();
                    List<String> dupList = new ArrayList<>(fileCodes.stream().map(MyBarcode::makeCodeKey).collect(Collectors.toList()));
                    dupList.retainAll(dbCodes.stream().map(MyBarcode::makeCodeKey).collect(Collectors.toList()));
                    int dup = dupList.size();

                    alertSave(activity, fileCount, dup,
                            dbCodes.stream().collect(Collectors.toMap(MyBarcode::makeCodeKey, Function.identity(), (p1, p2) -> p1)),
                            uri);
                }
            }

        } catch (Exception e) {
            LOG.e(TAG, "unzipBarcodes", e);
        }
    }

    private static void alertSave(Activity activity, int totalCount, int dupCount, Map<String, MyBarcode> dbCodes, Uri uri) {

        new AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.popup_confirm_restore, totalCount))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dupCount > 0) {
                            new AlertDialog.Builder(activity)
                                    .setMessage(activity.getString(R.string.popup_confirm_restore_dup, dupCount))
                                    .setPositiveButton(R.string.popup_confirm_restore_btn_overwrite, (dialog1, which1) ->
                                            restoreTask(activity, totalCount, uri, dbCodes, RestoreMode.OVERWRITE))
                                    .setNeutralButton(R.string.popup_confirm_restore_btn_add, (dialog13, which13) ->
                                            restoreTask(activity, totalCount, uri, dbCodes, RestoreMode.ADD))
                                    .setNegativeButton(R.string.popup_confirm_restore_btn_skip, (dialog12, which12) ->
                                            restoreTask(activity, totalCount, uri, dbCodes, RestoreMode.IGNORE))
                                    .create().show();
                        } else {
                            restoreTask(activity, totalCount, uri, dbCodes, RestoreMode.ADD);
                        }

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create().show();
    }

    private static void restoreTask(Activity activity, int count, Uri uri, Map<String, MyBarcode> dbCodes, RestoreMode mode) {
        AzApplication.executeJobOnBackground(new AzSimpleWorker() {
            @Override
            public void doInBackgroundAndResult() {

                int resultCount = restoreBarcodes(uri, dbCodes, mode);
                setResult(true, resultCount);

            }

            @Override
            public void postOperationWithResult(boolean result, Object value) {
                if (result) {
                    int resultCount = (int) value;
                    Toast.makeText(activity, activity.getString(R.string.toast_restore_barcode, resultCount), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public static int restoreBarcodes(Uri uri, Map<String, MyBarcode> dbCodes, RestoreMode mode) {
        AzAppDataHelper dataHelper = AzAppDataHelper.getInstance();

        int result = 0;

        try (ZipInputStream zis = new ZipInputStream(AzApplication.APP_CONTEXT.getContentResolver().openInputStream(uri))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry.getName().equals("barcode_summary")) {
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
            while (true) {
                int orgId = 0;
                MyBarcode barcode = null;
                if (entry.getName().startsWith("barcode_json_")) {
                    String json = IOUtils.toString(zis, StandardCharsets.UTF_8);
                    barcode = OBJECT_MAPPER.readValue(json, MyBarcode.class);
                    orgId = barcode.id;

                    if (!dbCodes.containsKey(barcode.makeCodeKey()) || mode == RestoreMode.ADD) {
                        int id = dataHelper.insertBarcode(barcode);
                        barcode.id = id;
                    } else if (mode == RestoreMode.OVERWRITE) {
                        MyBarcode dbCode = dbCodes.get(barcode.makeCodeKey());
                        barcode.id = dbCode.id;
                    } else {
                        // skip
                        while (true) {
                            zis.closeEntry();
                            entry = zis.getNextEntry();
                            if (entry == null || entry.getName().startsWith("barcode_json_")) {
                                break;
                            }
                        }

                        if (entry == null) {
                            break;
                        } else if (entry.getName().startsWith("barcode_json_")) {
                            continue;
                        }
                    }

                    barcode.barcodeImage = restoreDevicePath(barcode.barcodeImage, orgId, barcode.id);
                    barcode.originImage = restoreDevicePath(barcode.originImage, orgId, barcode.id);
                    barcode.coverImage = restoreDevicePath(barcode.coverImage, orgId, barcode.id);
                    dataHelper.updateBarcode(barcode);
                    result++;
                }
                zis.closeEntry();
                entry = zis.getNextEntry();

                while (restoreFile(zis, orgId, barcode.id, entry)) {
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                }

                if (entry == null) break;

            }
            AzApplication.APP_CONTEXT.getContentResolver().notifyChange(AzAppConstants.URI.BARCODE_LIST, null);
        } catch (Exception e) {
            LOG.e(TAG, "unzipBarcodes", e);
        }

        return result;
    }

    private static boolean restoreFile(ZipInputStream zis, int orgId, int newId, ZipEntry entry) {
        if (entry == null) {
            return false;
        }
        try {
            String fileName = entry.getName();
            if (fileName.contains("_" + orgId + "_")) {
                fileName = fileName.replace("_" + orgId + "_", "_" + newId + "_");
                String filePath = AzApplication.APP_CONTEXT.getFilesDir().getAbsolutePath() + "/" + fileName;
                File file = new File(filePath);
                if (file.exists()) file.delete();
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    IOUtils.copy(zis, fos);
                }
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            LOG.e(TAG, "restoreFile err", e);
            return false;
        }
    }

    public static enum RestoreMode {
        ADD, OVERWRITE, IGNORE
    }
}
