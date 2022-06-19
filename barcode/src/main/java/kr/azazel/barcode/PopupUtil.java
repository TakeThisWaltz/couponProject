package kr.azazel.barcode;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.net.Uri;

import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.LOG;
import com.google.android.gms.vision.barcode.Barcode;
import com.rey.material.app.DatePickerDialog;
import com.rey.material.app.DialogFragment;
import com.rey.material.app.ThemeManager;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import gun0912.tedbottompicker.TedBottomPicker;
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

    public static void showNewBarcodePopup(final AppCompatActivity activity, final Uri org, final Barcode code, final Bitmap codeImg, final Bitmap coverImg) {
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

                String expireDate = MetaManager.getInstance().getExtractedExpireDate();
                if (expireDate != null) {
                    try {
                        long time = new SimpleDateFormat("yyyy-MM-dd").parse(expireDate).getTime();
                        expirationDt = time;
                        tvExpirationDt.setText(expireDate);
                        selectedCategory = MyBarcode.Category.COUPON;
                        layoutExpireDt.setVisibility(View.VISIBLE);
                        MetaManager.getInstance().setExtractedExpireDate(null);
                    } catch (ParseException e) {
                        LOG.e(TAG, "expireDate parse error", e);
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

                ((TextView) dialog.findViewById(R.id.tv_code)).setText(code.rawValue);

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
                                boolean saved = MyBarcode.saveBarcode(selectedCategory.value(), code.rawValue, etTitle.getText().toString(), code.format
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
                                boolean isLightTheme = ThemeManager.getInstance().getCurrentTheme() == 0;
                                DatePickerDialog.Builder builder = new DatePickerDialog.Builder(isLightTheme ? R.style.Material_App_Dialog_DatePicker_Light : R.style.Material_App_Dialog_DatePicker) {
                                    @Override
                                    public void onPositiveActionClicked(DialogFragment fragment) {
                                        DatePickerDialog dateDialog = (DatePickerDialog) fragment.getDialog();
                                        expirationDt = dateDialog.getDate();

                                        tvExpirationDt.setText(AzUtil.getLongDateStringFromMils(dialog.getContext(), expirationDt, false));

                                        super.onPositiveActionClicked(fragment);
                                    }

                                    @Override
                                    public void onNegativeActionClicked(DialogFragment fragment) {
                                        super.onNegativeActionClicked(fragment);
                                    }
                                };

                                if (expirationDt > 0)
                                    builder.date(expirationDt);
                                builder.positiveAction(activity.getString(android.R.string.ok))
                                        .negativeAction(activity.getString(android.R.string.cancel));

                                DialogFragment fragment = DialogFragment.newInstance(builder);
                                fragment.show(activity.getSupportFragmentManager(), TAG);
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
                                boolean isLightTheme = ThemeManager.getInstance().getCurrentTheme() == 0;
                                DatePickerDialog.Builder builder = new DatePickerDialog.Builder(isLightTheme ? R.style.Material_App_Dialog_DatePicker_Light : R.style.Material_App_Dialog_DatePicker) {
                                    @Override
                                    public void onPositiveActionClicked(DialogFragment fragment) {
                                        DatePickerDialog dateDialog = (DatePickerDialog) fragment.getDialog();
                                        barcode.expirationDate = dateDialog.getDate();

                                        tvExpirationDt.setText(AzUtil.getLongDateStringFromMils(dialog.getContext(), barcode.expirationDate, false));

                                        super.onPositiveActionClicked(fragment);
                                    }

                                    @Override
                                    public void onNegativeActionClicked(DialogFragment fragment) {
                                        super.onNegativeActionClicked(fragment);
                                    }
                                };

                                if (barcode.expirationDate > 0)
                                    builder.date(barcode.expirationDate);
                                builder.positiveAction(activity.getString(android.R.string.ok))
                                        .negativeAction(activity.getString(android.R.string.cancel));

                                DialogFragment fragment = DialogFragment.newInstance(builder);
                                fragment.show(activity.getSupportFragmentManager(), TAG);
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
