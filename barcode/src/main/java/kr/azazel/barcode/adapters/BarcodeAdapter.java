package kr.azazel.barcode.adapters;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.azazel.framework.AzApplication;
import com.azazel.framework.util.LOG;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import kr.azazel.barcode.FileUtil;
import kr.azazel.barcode.PopupUtil;
import kr.azazel.barcode.R;
import kr.azazel.barcode.view.FoldableLayout;
import kr.azazel.barcode.vo.MyBarcode;

/**
 * Created by ji on 2016. 10. 13..
 */

public class BarcodeAdapter implements ICursorAdapter {
    public static final String TAG = "ChannelAdapter";

    private Map<Integer, Boolean> mFoldStates = new HashMap<>();

    private Activity activity;

    private MyCursorAdapter adaper;
    private ICursorAdapter.IDataLoadLisner dataLoadLisner;

    public BarcodeAdapter(final Activity activity, int id, Uri uri, ICursorAdapter.IDataLoadLisner dataLoadLisner) {
        this.activity = activity;
        this.dataLoadLisner = dataLoadLisner;
        adaper = new MyCursorAdapter(activity, id, uri, this);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LOG.d(TAG, "newView : " + cursor);
        FoldableLayout view = new FoldableLayout(context);

        ViewHolder holder = new ViewHolder(view);

        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(final View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final MyBarcode barcode = new MyBarcode(cursor);
        LOG.d(TAG, "bindView : " + barcode);


//                holder.mImageViewCover.setImageBitmap(BitmapFactory.decodeFile(barcode.barcodeImage));
//                holder.mImageViewDetail.setImageBitmap(BitmapFactory.decodeFile(barcode.barcodeImage));
//                // Bind data
        final Uri imageCode = Uri.fromFile(new File(barcode.barcodeImage));
        Picasso.with(holder.mFoldableLayout.getContext()).load(imageCode).into(holder.imgDetail);

        if (TextUtils.isEmpty(barcode.coverImage)) {
            holder.imgCover.setImageResource(R.mipmap.ic_default);
        } else {
            final Uri imageCover = Uri.fromFile(new File(barcode.coverImage));
            Picasso.with(holder.mFoldableLayout.getContext()).load(imageCover).into(holder.imgCover);
        }
        holder.tvCoverTitle.setText(barcode.title);
        holder.tvDetailTitle.setText(barcode.title);
        holder.tvDetailCode.setText(barcode.code + "/" + barcode.type + "\uD83D\uDCCB");
        holder.tvDetailCode.setTag(barcode.code);

        holder.tvDetailCode.setOnClickListener((v) -> {
            ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("label", ((TextView) v).getTag().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(AzApplication.APP_CONTEXT, R.string.toast_copied, Toast.LENGTH_SHORT).show();
        });

        holder.tvDelete.setTag(barcode);
        holder.tvDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.btn_delete_alert)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((MyBarcode) v.getTag()).delete();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create().show();
        });

        holder.tvCoverDesc.setText(barcode.description);
        if (barcode.expirationDate > 0) {
            holder.tvCoverExpired.setText(android.text.format.DateFormat.format(context.getString(R.string.expiredt_format), barcode.expirationDate));
            holder.tvCoverExpired.setVisibility(View.VISIBLE);
        } else holder.tvCoverExpired.setVisibility(View.GONE);

        // Bind state
        if (mFoldStates.containsKey(barcode.id)) {
            if (mFoldStates.get(barcode.id) == Boolean.TRUE) {
                if (!holder.mFoldableLayout.isFolded()) {
                    holder.mFoldableLayout.foldWithoutAnimation();
                }
            } else if (mFoldStates.get(barcode.id) == Boolean.FALSE) {
                if (holder.mFoldableLayout.isFolded()) {
                    holder.mFoldableLayout.unfoldWithoutAnimation();
                }
            }
        } else {
            holder.mFoldableLayout.foldWithoutAnimation();
        }

        holder.tvShare.setTag(barcode);
        holder.tvShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileUtil.shareLocalFile(((MyBarcode) v.getTag()).originImage);
            }
        });
        holder.tvEdit.setTag(barcode);
        holder.tvEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupUtil.showEditBarcodePopup((AppCompatActivity) activity, ((MyBarcode) v.getTag()));
            }
        });

        holder.mFoldableLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.mFoldableLayout.isFolded()) {
                    holder.mFoldableLayout.unfoldWithAnimation();
                } else {
                    holder.mFoldableLayout.foldWithAnimation();
                }
            }
        });
        holder.mFoldableLayout.setFoldListener(new FoldableLayout.FoldListener() {
            @Override
            public void onUnFoldStart() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.mFoldableLayout.setElevation(5);
                }
            }

            @Override
            public void onUnFoldEnd() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.mFoldableLayout.setElevation(0);
                }
                mFoldStates.put(barcode.id, false);
            }

            @Override
            public void onFoldStart() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.mFoldableLayout.setElevation(5);
                }
            }

            @Override
            public void onFoldEnd() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.mFoldableLayout.setElevation(0);
                }
                mFoldStates.put(barcode.id, true);
            }
        });


    }

    public BaseAdapter getAdapter() {
        return adaper.getAdapter();
    }

    @Override
    public void onDataLoadFinished(int dataCount) {
        dataLoadLisner.onDataLoadFinished(dataCount);
    }

    static class ViewHolder {
        protected FoldableLayout mFoldableLayout;

        @Bind(R.id.img_cover)
        protected ImageView imgCover;

        @Bind(R.id.img_detail)
        protected ImageView imgDetail;

        @Bind(R.id.tv_detail_code)
        protected TextView tvDetailCode;

        @Bind(R.id.tv_detail_title)
        protected TextView tvDetailTitle;

        @Bind(R.id.tv_cover_title)
        protected TextView tvCoverTitle;

        @Bind(R.id.tv_cover_desc)
        protected TextView tvCoverDesc;

        @Bind(R.id.tv_expiredt)
        protected TextView tvCoverExpired;

//        @Bind(R.id.btn_edit)
//        protected ImageButton btnEdit;

        private TextView tvShare;
        private TextView tvEdit;
        private TextView tvDelete;

        public ViewHolder(FoldableLayout foldableLayout) {
            mFoldableLayout = foldableLayout;
            foldableLayout.setupViews(R.layout.list_item_cover, R.layout.list_item_detail, R.dimen.card_cover_height, mFoldableLayout.getContext());
            ButterKnife.bind(this, foldableLayout);
            this.imgCover = (ImageView) foldableLayout.findViewById(R.id.img_cover);
            this.imgDetail = (ImageView) foldableLayout.findViewById(R.id.img_detail);
            this.tvDetailCode = (TextView) foldableLayout.findViewById(R.id.tv_detail_code);
            this.tvDetailTitle = (TextView) foldableLayout.findViewById(R.id.tv_detail_title);
            this.tvCoverTitle = (TextView) foldableLayout.findViewById(R.id.tv_cover_title);
            this.tvCoverDesc = (TextView) foldableLayout.findViewById(R.id.tv_cover_desc);
            this.tvCoverExpired = (TextView) foldableLayout.findViewById(R.id.tv_expiredt);
//            this.btnEdit = foldableLayout.findViewById(R.id.btn_edit);

            this.tvShare = (TextView) foldableLayout.findViewById(R.id.tv_share);
            this.tvEdit = (TextView) foldableLayout.findViewById(R.id.tv_edit);
            this.tvDelete = (TextView) foldableLayout.findViewById(R.id.tv_delete);
        }
    }

}