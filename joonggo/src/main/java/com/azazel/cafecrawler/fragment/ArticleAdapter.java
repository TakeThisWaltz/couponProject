package com.azazel.cafecrawler.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.azazel.cafecrawler.AlarmManager;
import com.azazel.cafecrawler.CrawlConstants;
import com.azazel.cafecrawler.CrawlManager;
import com.azazel.cafecrawler.MainActivity;
import com.azazel.cafecrawler.R;
import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.framework.AzApplication;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.LOG;
import com.azazel.framework.view.ImageViewSwitch;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

/**
 * Created by JJ on 2015-03-18.
 */
public class ArticleAdapter extends ArrayAdapter<CrawlDataHelper.Article> {
    public static final String TAG = "ArticleAdapter";
    private int titleRes;
    private int infoRes;
    private int thmbRes;
    private boolean isEditable = true;
    private CrawlDataHelper mDataHelper;
    private AlarmManager mAlarmMgr;

    private DisplayImageOptions mUILOptions = new DisplayImageOptions.Builder()
            .showImageOnLoading(null)
            .showImageForEmptyUri(null)
            .showImageOnFail(null)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build();

    private long mBeforeMaxArticleId;

    public ArticleAdapter(Context context, int resource, int textResId, List<CrawlDataHelper.Article> data, int titleRes, int infoRes, int thmbRes) {
        super(context, resource, textResId, data);
        LOG.f(TAG, "init ArticleAdapter ");
        this.titleRes = titleRes;
        this.infoRes = infoRes;
        this.thmbRes = thmbRes;
        mDataHelper = CrawlDataHelper.getInstance();
        mAlarmMgr = AlarmManager.getInstance();
    }

    public void setBeforeMaxArticleId(long articleId) {
        mBeforeMaxArticleId = articleId;
    }

    class ViewHolder {
        ImageView icNew;
        ImageView thumb;
        TextView title;
        TextView writer;
        TextView price;

        View scrapSet;
        ImageViewSwitch chkAlarm;
        View btnDel;
        ImageViewSwitch chkReUp;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        ViewHolder holder = (ViewHolder) view.getTag();

        if (holder == null) {
            holder = new ViewHolder();
            holder.title = (TextView) view.findViewById(titleRes);
            holder.writer = (TextView) view.findViewById(infoRes);
            holder.price = (TextView) view.findViewById(R.id.tv_list_price);
            holder.thumb = (ImageView) view.findViewById(thmbRes);
            if (isEditable) {
                holder.scrapSet = view.findViewById(R.id.layout_scrap_set);
                if (holder.scrapSet == null)
                    isEditable = false;
                else {
                    holder.scrapSet.setVisibility(View.VISIBLE);
                    holder.icNew = (ImageView) view.findViewById(R.id.img_ic_new);
                    holder.chkAlarm = new ImageViewSwitch((ImageView) view.findViewById(R.id.chk_scrap_alarm), new ImageViewSwitch.OnImageCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(ImageViewSwitch view, boolean isChecked) {
                            CrawlDataHelper.Article item = (CrawlDataHelper.Article) view.getTag();
                            if (item.alarm != isChecked) {
                                item.alarm = isChecked;
                                mAlarmMgr.setCommentAlarm(item.articleId, item.alarm);
                                Toast.makeText(AzApplication.APP_CONTEXT, isChecked ? R.string.toast_comment_alarm_on : R.string.toast_comment_alarm_off, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    holder.chkReUp = new ImageViewSwitch((ImageView) view.findViewById(R.id.chk_reup), new ImageViewSwitch.OnImageCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(final ImageViewSwitch view, final boolean isChecked) {
                            final CrawlDataHelper.Article item = (CrawlDataHelper.Article) view.getTag();
                            if(item.autoReUpload == -2) {
                                view.setChecked(!view.getChecked());
                                Toast.makeText(AzApplication.APP_CONTEXT, "현재 끌어올리기 실행중 입니다.", Toast.LENGTH_LONG).show();
                            }else if(item.autoReUpload == -1) {
                                    view.setChecked(!view.getChecked());
                                    Toast.makeText(AzApplication.APP_CONTEXT, R.string.toast_reup_failed, Toast.LENGTH_LONG).show();
                            }else if ((item.autoReUpload > 0) != isChecked) {
                                if(isChecked == true) {

                                    AlertDialog.Builder alert_confirm = new AlertDialog.Builder(view.getContext());
                                    alert_confirm.setMessage("끌어올리기 실행").setCancelable(true)
                                            .setPositiveButton("지금 끌어올리기",
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            view.setChecked(!view.getChecked());
                                                            item.autoReUpload = -2;
                                                            CrawlManager.getInstance().reUp(item, true);
                                                        }
                                                    }).setNegativeButton("예약 끌어올리기",
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    item.autoReUpload = isChecked ? System.currentTimeMillis() : 0;
                                                    mAlarmMgr.setReUpAlarm(item.articleId, isChecked);
                                                    Toast.makeText(AzApplication.APP_CONTEXT, isChecked ? R.string.toast_reup_on : R.string.toast_reup_off, Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialogInterface) {
                                            view.setChecked(!view.getChecked());
                                        }
                                    });
                                    AlertDialog alert = alert_confirm.create();
                                    alert.show();

                                }else{
                                    item.autoReUpload = isChecked?System.currentTimeMillis():0;
                                    mAlarmMgr.setReUpAlarm(item.articleId, isChecked);
                                    Toast.makeText(AzApplication.APP_CONTEXT, isChecked ? R.string.toast_reup_on : R.string.toast_reup_off, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
                    holder.btnDel = view.findViewById(R.id.btn_scrap_delete);


                    holder.btnDel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CrawlDataHelper.Article item = (CrawlDataHelper.Article) v.getTag();
                            mAlarmMgr.deleteScrap(item.articleId);
                            AzApplication.sendEmptyMessage(MainActivity.TAG, CrawlConstants.Event.SCRAP_CHANGED);
                        }
                    });

                }
            }

            view.setTag(holder);
        }

        CrawlDataHelper.Article item = getItem(position);

        holder.title.setText(item.title);
        holder.writer.setText(item.writer + "|" + item.date + (item.comment>0?"|[" + item.comment + "]":""));
        if(holder.price != null){
            if(item.price == null) holder.price.setVisibility(View.GONE);
            else{
                holder.price.setVisibility(View.VISIBLE);
                holder.price.setText("가격: "+item.price);
            }
        }
        holder.title.setText(item.title);
        if (!AzUtil.isNullOrEmpty(item.thumb)) {
            holder.thumb.setVisibility(View.VISIBLE);
            ImageLoader.getInstance().displayImage(item.thumb, holder.thumb, mUILOptions);
        } else holder.thumb.setVisibility(View.GONE);

        if (isEditable) {
            holder.icNew.setVisibility(View.GONE);
            switch (item.type) {
                case CrawlConstants.ArticleType.SEARCH_RESULT: {
                    holder.scrapSet.setVisibility(View.GONE);
                    if (mBeforeMaxArticleId > 0 && item.articleId > mBeforeMaxArticleId)
                        holder.icNew.setVisibility(View.VISIBLE);
                    break;
                }
                case CrawlConstants.ArticleType.SCRAP: {
                    holder.scrapSet.setVisibility(View.VISIBLE);
                    holder.chkAlarm.setVisibility(item.isDeleted?View.GONE:View.VISIBLE);
                    holder.chkReUp.setVisibility(View.GONE);
                    holder.btnDel.setVisibility(View.VISIBLE);
                    break;
                }
                case CrawlConstants.ArticleType.MY_COMMENT: {
                    holder.scrapSet.setVisibility(View.VISIBLE);
                    holder.chkAlarm.setVisibility(View.VISIBLE);
                    holder.chkReUp.setVisibility(View.GONE);
                    holder.btnDel.setVisibility(View.GONE);
                    break;
                }
                case CrawlConstants.ArticleType.MY_ARTICLE: {
                    holder.scrapSet.setVisibility(View.VISIBLE);
                    holder.chkAlarm.setVisibility(View.VISIBLE);
                    holder.chkReUp.setVisibility(View.VISIBLE);
                    holder.btnDel.setVisibility(View.GONE);
                    break;
                }
            }

            holder.chkAlarm.setTag(item);
            holder.chkReUp.setTag(item);
            holder.btnDel.setTag(item);
            holder.chkAlarm.setChecked(item.alarm);

            //holder.chkReUp.setEnabled(item.autoReUpload >= 0);

            holder.chkReUp.setChecked(item.autoReUpload > 0);

        }


        return view;
    }
}