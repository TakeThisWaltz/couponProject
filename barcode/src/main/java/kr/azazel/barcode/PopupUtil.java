package kr.azazel.barcode;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.LOG;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import gun0912.tedbottompicker.TedBottomPicker;
import kr.azazel.barcode.vo.BarcodeResponse;
import kr.azazel.barcode.vo.BarcodeVo;
import kr.azazel.barcode.vo.MyBarcode;

/**
 * Created by ji on 2016. 10. 28..
 */

public class PopupUtil {
    private static final String TAG = "PopupUtil";

    public static void showCoverImageCropPopup(final Activity activity, final Uri org, final CropImageView.OnCropImageCompleteListener listener) {
        if (org != null) {
            AzUtil.makeTransparentBackgroundDialog(activity, R.layout.popup_crop, new AzUtil.OnAzDialogCreateListener() {
                @Override
                public void onAzDialogCreated(final Dialog dialog) {
                    final CropImageView cropImageView = (CropImageView) dialog.findViewById(R.id.img_crop);
                    cropImageView.setAspectRatio(AzAppConstants.BARCODE_IMG_WIDTH, AzAppConstants.BARCODE_IMG_HEIGHT);
                    cropImageView.setFixedAspectRatio(true);
                    cropImageView.setScaleType(CropImageView.ScaleType.FIT_CENTER);
                    cropImageView.setAutoZoomEnabled(true);
//                    try {
//                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), org);
//                        if(bitmap != null) {
//                            int displayWidth = MetaManager.getInstance().getDisplayWidth();
//                            int displayHeight = MetaManager.getInstance().getDisplayHeight();
//
//                            LOG.i(TAG, "bitmap size : " + bitmap.getWidth() + ", " + bitmap.getHeight() + ".. display : " + displayWidth + ", " + displayHeight);
//
//                            ViewGroup.LayoutParams layoutParams = cropImageView.getLayoutParams();
//                            if (bitmap.getWidth() >= bitmap.getHeight()) {
//                                layoutParams.height = (displayWidth * bitmap.getHeight()) / (2 * bitmap.getWidth());
//                                layoutParams.width = displayWidth / 2;
//                            } else {
//                                layoutParams.height = displayHeight / 2;
//                                layoutParams.width = (displayHeight * bitmap.getWidth()) / (2 * bitmap.getHeight());
//                            }
//
//                            cropImageView.setLayoutParams(layoutParams);
//                            cropImageView.setImageBitmap(bitmap);//.setImageURI(Uri.fromFile(new File(smallFileName)));
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }


                    cropImageView.setImageUriAsync(org);
                    cropImageView.setOnSetImageUriCompleteListener(new CropImageView.OnSetImageUriCompleteListener() {
                        @Override
                        public void onSetImageUriComplete(CropImageView view, Uri uri, Exception error) {
                            LOG.d(TAG, "onSetImageUriComplete - " + uri);
                        }
                    });
                    cropImageView.setOnCropImageCompleteListener(new CropImageView.OnCropImageCompleteListener() {
                        @Override
                        public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
                            LOG.d(TAG, "onCropImageComplete - " + (result == null ? "null" : result.isSuccessful()));
                            if (result != null && result.isSuccessful()) {
                                dialog.cancel();
                                listener.onCropImageComplete(view, result);
                            }
                        }
                    });

                    dialog.findViewById(R.id.btn_crop).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            cropImageView.getCroppedImageAsync(AzAppConstants.BARCODE_IMG_WIDTH, AzAppConstants.BARCODE_IMG_HEIGHT);

                        }
                    });
                    dialog.findViewById(R.id.btn_skip).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            listener.onCropImageComplete(cropImageView, null);
                            dialog.cancel();
                        }
                    });
                }
            }).show();
        }
    }

    public static void showNewBarcodePopup(final AppCompatActivity activity, final Uri org, final BarcodeVo code, final Bitmap codeImg, final Bitmap coverImg) {
        AzUtil.makeTransparentBackgroundDialog(activity, R.layout.popup_new_barcode, new AzUtil.OnAzDialogCreateListener() {
            MyBarcode.Category selectedCategory = MyBarcode.Category.MEMBERSHIP;
            long expirationDt = 0;
            private TedBottomPicker imagePicker;
            private Bitmap bitmapCover;

            @Override
            public void onAzDialogCreated(final Dialog dialog) {
                final ImageView imgCover = (ImageView) dialog.findViewById(R.id.img_cover);
                ImageView imgCode = (ImageView) dialog.findViewById(R.id.img_barcode);
                final EditText etTitle = (EditText) dialog.findViewById(R.id.et_title);
                final EditText etDesc = (EditText) dialog.findViewById(R.id.et_desc);
                final EditText etBrand = (EditText) dialog.findViewById(R.id.et_brand);
                final TextView tvExpirationDt = (TextView) dialog.findViewById(R.id.tv_expiredt_value);
                final View layoutExpireDt = dialog.findViewById(R.id.layout_expiredt);

                bitmapCover = coverImg;

                final RadioGroup cateSel = (RadioGroup) dialog.findViewById(R.id.radio_category);

                BarcodeResponse barcodeResponse = MetaManager.getInstance().getTempBarcode();
                if(barcodeResponse != null){
                    try {
                        etTitle.setText(barcodeResponse.getStore());
                        etDesc.setText(barcodeResponse.getItem());
                        long time = new SimpleDateFormat("yyyy-MM-dd").parse(barcodeResponse.getExpireDate()).getTime();
                        expirationDt = time;
                        tvExpirationDt.setText(barcodeResponse.getExpireDate());
                        selectedCategory = barcodeResponse.getCategory();
                        if(selectedCategory == MyBarcode.Category.COUPON) {
                            layoutExpireDt.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        LOG.e(TAG, "expireDate parse error", e);
                    }finally {
                        MetaManager.getInstance().setTempBarcode(null);
                    }
                }

                for (MyBarcode.Category cate : MyBarcode.Category.values()) {
                    if (cate == MyBarcode.Category.TOTAL) continue;

                    RadioButton r = new RadioButton(dialog.getContext());
                    r.setText(cate.displayString());
                    r.setTag(cate);
                    cateSel.addView(r);
                    if (cate == selectedCategory) r.setChecked(true);
                    r.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                selectedCategory = (MyBarcode.Category) buttonView.getTag();
                                layoutExpireDt.setVisibility(selectedCategory == MyBarcode.Category.COUPON ? View.VISIBLE : View.GONE);
                            }
                        }
                    });
                }

                ((TextView) dialog.findViewById(R.id.tv_code)).setText(code.getRawValue());

                if (bitmapCover != null)
                    imgCover.setImageBitmap(bitmapCover);
                else
                    imgCover.setImageResource(R.mipmap.ic_default);

                imgCode.setImageBitmap(codeImg);

                imagePicker = new TedBottomPicker.Builder(activity)
                        .setOnImageSelectedListener(new TedBottomPicker.OnImageSelectedListener() {
                            @Override
                            public void onImageSelected(Uri uri) {
                                LOG.d(TAG, "onImageSelected - uri : " + uri);
                                if (uri != null) {
                                    showCoverImageCropPopup(activity, uri, new CropImageView.OnCropImageCompleteListener() {
                                        @Override
                                        public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
                                            LOG.d(TAG, "onCropImageComplete - result : " + result);
                                            if (result != null) {
                                                bitmapCover = result.getBitmap();
                                                imgCover.setImageBitmap(bitmapCover);
                                            }
                                        }
                                    });
                                } else {

                                }
                            }
                        })
                        .create();


                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (v.getId()) {
                            case R.id.btn_ok: {
                                Bundle bundle = new Bundle();
                                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "barcode_save");
                                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, selectedCategory.value() + "");
                                FirebaseAnalytics.getInstance(v.getContext())
                                        .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);


                                boolean saved = MyBarcode.saveBarcode(selectedCategory.value(), code.getRawValue(), etTitle.getText().toString(), code.getFormat()
                                        , etDesc.getText().toString(), etBrand.getText().toString(), org, codeImg, bitmapCover, expirationDt);

                                if (saved && org != null) {
//                                    new AlertDialog.Builder(activity)
//                                            .setMessage(R.string.popup_del_org)
//                                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                                                @Override
//                                                public void onClick(DialogInterface dialog, int which) {
//                                                    try {
//                                                        if ("file".equals(org.getScheme())) {
//                                                            new File(org.getPath()).delete();
//                                                            MediaScannerConnection.scanFile(activity, new String[]{org.getPath()}, null, null);
//                                                        } else if ("content".equals(org.getScheme())) {
//                                                            activity.getContentResolver().delete(org, null, null);
//                                                        }
//
//                                                    } catch (Exception e) {
//                                                        LOG.e(TAG, "delete err", e);
//                                                        Toast.makeText(activity, R.string.toast_del_failed, Toast.LENGTH_SHORT);
//                                                    }
//                                                }
//                                            })
//                                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
//                                                @Override
//                                                public void onClick(DialogInterface dialog, int which) {
//
//                                                }
//                                            })
//                                            .create().show();
                                }

                                dialog.cancel();
                                break;
                            }
                            case R.id.btn_cancel: {
                                dialog.cancel();
                                break;
                            }
                            case R.id.img_cover: {
                                imagePicker.show(((AppCompatActivity) activity).getSupportFragmentManager());
                                break;
                            }
                            case R.id.tv_expiredt_reset: {
                                expirationDt = 0;
                                tvExpirationDt.setText(R.string.no_expiredt);
                                break;
                            }
                            case R.id.tv_expiredt_value:
                            case R.id.tv_expiredt: {
                                LocalDateTime localDateTime = expirationDt > 0 ?
                                        LocalDateTime.ofInstant(Instant.ofEpochMilli(expirationDt), ZoneId.systemDefault()) :
                                        LocalDateTime.now();

                                DatePickerDialog dialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                                        expirationDt = LocalDate.of(i, i1 + 1, i2).atTime(0, 0)
                                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                                        tvExpirationDt.setText(AzUtil.getLongDateStringFromMils(activity, expirationDt, false));
                                    }
                                }, localDateTime.getYear(), localDateTime.getMonthValue() - 1, localDateTime.getDayOfMonth());
                                dialog.show();

                                break;
                            }
                        }
                    }
                };

                imgCover.setOnClickListener(clickListener);

                dialog.findViewById(R.id.btn_ok).setOnClickListener(clickListener);
                dialog.findViewById(R.id.btn_cancel).setOnClickListener(clickListener);

                dialog.findViewById(R.id.tv_expiredt).setOnClickListener(clickListener);
                dialog.findViewById(R.id.tv_expiredt_value).setOnClickListener(clickListener);
                dialog.findViewById(R.id.tv_expiredt_reset).setOnClickListener(clickListener);
            }
        }).show();
    }


    public static void showEditBarcodePopup(final AppCompatActivity activity, final MyBarcode barcode) {
        AzUtil.makeTransparentBackgroundDialog(activity, R.layout.popup_edit_barcode, new AzUtil.OnAzDialogCreateListener() {

            private MyBarcode.Category selectedCategory;

            private TedBottomPicker imagePicker;
            private TedBottomPicker.OnImageSelectedListener onImageSelectedListener;

            private Bitmap bitmapCover;

            @Override
            public void onAzDialogCreated(final Dialog dialog) {
                final ImageView imgCover = (ImageView) dialog.findViewById(R.id.img_cover);
                ImageView imgCode = (ImageView) dialog.findViewById(R.id.img_barcode);
                final EditText etTitle = (EditText) dialog.findViewById(R.id.et_title);
                final EditText etDesc = (EditText) dialog.findViewById(R.id.et_desc);
                final EditText etBrand = (EditText) dialog.findViewById(R.id.et_brand);
                final TextView tvExpirationDt = (TextView) dialog.findViewById(R.id.tv_expiredt_value);
                final View layoutExpireDt = dialog.findViewById(R.id.layout_expiredt);

                etTitle.setText(barcode.title);
                etDesc.setText(barcode.description);
                etBrand.setText(barcode.brand);

                final RadioGroup cateSel = (RadioGroup) dialog.findViewById(R.id.radio_category);

                selectedCategory = MyBarcode.Category.fromValue(barcode.category);
                layoutExpireDt.setVisibility(selectedCategory == MyBarcode.Category.COUPON ? View.VISIBLE : View.GONE);

                for (MyBarcode.Category cate : MyBarcode.Category.values()) {
                    if (cate == MyBarcode.Category.TOTAL) continue;

                    RadioButton r = new RadioButton(dialog.getContext());
                    r.setText(cate.displayString());
                    r.setTag(cate);
                    cateSel.addView(r);
                    if (cate == selectedCategory) r.setChecked(true);

                    r.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            LOG.d(TAG, "onCheckedChanged - " + isChecked);
                            if (isChecked) {
                                selectedCategory = (MyBarcode.Category) buttonView.getTag();
                                layoutExpireDt.setVisibility(selectedCategory == MyBarcode.Category.COUPON ? View.VISIBLE : View.GONE);
                            }
                        }
                    });
                }

                ((TextView) dialog.findViewById(R.id.tv_code)).setText(barcode.code);

                final Uri imageCode = Uri.fromFile(new File(barcode.barcodeImage));
                Picasso.with(activity).load(imageCode).into(imgCode);

                if (TextUtils.isEmpty(barcode.coverImage)) {
                    imgCover.setImageResource(R.mipmap.ic_default);
                } else {
                    final Uri imageCover = Uri.fromFile(new File(barcode.coverImage));
                    Picasso.with(activity).load(imageCover).into(imgCover);
                }

                onImageSelectedListener = new TedBottomPicker.OnImageSelectedListener() {
                    @Override
                    public void onImageSelected(Uri uri) {
                        LOG.d(TAG, "onImageSelected - uri : " + uri);

                        showCoverImageCropPopup(activity, uri, new CropImageView.OnCropImageCompleteListener() {
                            @Override
                            public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
                                LOG.d(TAG, "onCropImageComplete - result : " + result);
                                if (result != null) {
                                    bitmapCover = result.getBitmap();
                                    imgCover.setImageBitmap(bitmapCover);
                                }
                            }
                        });

                    }
                };

                imagePicker = new TedBottomPicker.Builder(activity)
                        .setOnImageSelectedListener(onImageSelectedListener)
                        .create();

                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (v.getId()) {
                            case R.id.btn_ok: {
                                barcode.title = etTitle.getText().toString();
                                barcode.description = etDesc.getText().toString();
                                barcode.category = selectedCategory.value();

                                barcode.update(bitmapCover);
                                dialog.cancel();
                                break;
                            }
                            case R.id.btn_close: {
                                dialog.cancel();
                                break;
                            }
                            case R.id.img_cover: {
                                imagePicker.show(((AppCompatActivity) activity).getSupportFragmentManager());
                                break;
                            }
                            case R.id.tv_expiredt_reset: {
                                barcode.expirationDate = 0;
                                tvExpirationDt.setText(R.string.no_expiredt);
                                break;
                            }
                            case R.id.tv_expiredt_value:
                            case R.id.tv_expiredt: {
                                LocalDateTime localDateTime = barcode.expirationDate > 0 ?
                                        LocalDateTime.ofInstant(Instant.ofEpochMilli(barcode.expirationDate), ZoneId.systemDefault()) :
                                        LocalDateTime.now();

                                DatePickerDialog dialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                                        barcode.expirationDate = LocalDate.of(i, i1 + 1, i2).atTime(0, 0)
                                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                                        tvExpirationDt.setText(AzUtil.getLongDateStringFromMils(activity, barcode.expirationDate, false));
                                    }
                                }, localDateTime.getYear(), localDateTime.getMonthValue() - 1, localDateTime.getDayOfMonth());
                                dialog.show();
                                break;
                            }
                        }
                    }
                };


                imgCover.setOnClickListener(clickListener);

                dialog.findViewById(R.id.btn_ok).setOnClickListener(clickListener);
                dialog.findViewById(R.id.btn_close).setOnClickListener(clickListener);

                dialog.findViewById(R.id.tv_expiredt).setOnClickListener(clickListener);
                dialog.findViewById(R.id.tv_expiredt_value).setOnClickListener(clickListener);
                dialog.findViewById(R.id.tv_expiredt_reset).setOnClickListener(clickListener);

                if (barcode.expirationDate > 0)
                    tvExpirationDt.setText(AzUtil.getLongDateStringFromMils(dialog.getContext(), barcode.expirationDate, false));
            }
        }).show();
    }
}
