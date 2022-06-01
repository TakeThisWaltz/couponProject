package com.azazel.cafecrawler.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.azazel.cafecrawler.AlarmManager;
import com.azazel.cafecrawler.CrawlManager;
import com.azazel.cafecrawler.MainActivity;
import com.azazel.cafecrawler.MetaManager;
import com.azazel.cafecrawler.R;
import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.framework.util.LOG;
import com.rey.material.app.DialogFragment;
import com.rey.material.app.TimePickerDialog;

import java.util.Calendar;

/**
 * Created by JJ on 2015-03-18.
 */
public class SettingFragment extends Fragment implements ICrawlFragment {
    private static final String TAG = "SettingFragment";

    private MetaManager mMeta;
    private AlarmManager mAlarmMgr;
    private CrawlDataHelper mDataHelper;
    private CrawlManager mCrawlMgr;
    private MainActivity mMainActivity;

    private View mView;

    private TextView tvFromAmPm;
    private TextView tvFromTime;
    private TextView tvToAmPm;
    private TextView tvToTime;

    private String[] mReupIntervalTitle = {
            "2 시간마다",
            "3 시간마다",
            "6 시간마다",
            "12 시간마다",
            "24 시간마다"
    };

    @Override
    public String getTitle() {
        return "설정";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG.f(TAG, "onCreateView : ");

        mMeta = MetaManager.getInstance();
        mDataHelper = CrawlDataHelper.getInstance();
        mAlarmMgr = AlarmManager.getInstance();
        mCrawlMgr = CrawlManager.getInstance();
        mMainActivity = (MainActivity) this.getActivity();


        mView = inflater.inflate(R.layout.frag_setting, container, false);

        SwitchCompat chkTimezone = (SwitchCompat) mView.findViewById(R.id.chk_alarm_timezone);
        chkTimezone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked != mMeta.hasAlarmTimezone())
                    mAlarmMgr.setHasAlarmTimezone(isChecked);
                mView.findViewById(R.id.layout_timezone).setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        chkTimezone.setChecked(mMeta.hasAlarmTimezone());

        tvFromAmPm = (TextView) mView.findViewById(R.id.tv_timezone_from_ampm);
        tvFromTime = (TextView) mView.findViewById(R.id.tv_timezone_from_time);
        tvToAmPm = (TextView) mView.findViewById(R.id.tv_timezone_to_ampm);
        tvToTime = (TextView) mView.findViewById(R.id.tv_timezone_to_time);

        Calendar fromTime = mMeta.getAlarmTimezoneFrom();
        Calendar toTime = mMeta.getAlarmTimezoneTo();

        tvFromAmPm.setText(fromTime.get(Calendar.AM_PM) == Calendar.AM ? R.string.label_am : R.string.label_pm);
        tvFromTime.setText(getTimeString(fromTime));
        tvToAmPm.setText(toTime.get(Calendar.AM_PM) == Calendar.AM ? R.string.label_am : R.string.label_pm);
        tvToTime.setText(getTimeString(toTime));

        tvFromAmPm.setTag(fromTime);
        tvFromTime.setTag(fromTime);
        tvToAmPm.setTag(toTime);
        tvToTime.setTag(toTime);

        tvFromAmPm.setOnClickListener(mTimezoneListener);
        tvFromTime.setOnClickListener(mTimezoneListener);
        tvToAmPm.setOnClickListener(mTimezoneListener);
        tvToTime.setOnClickListener(mTimezoneListener);

        SwitchCompat chkVibration = (SwitchCompat) mView.findViewById(R.id.chk_alarm_vibrate);
        chkVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked != mMeta.isVibrationAlarm())
                    mMeta.setVibrationAlarm(isChecked);
            }
        });
        chkVibration.setChecked(mMeta.isVibrationAlarm());


        Spinner spnReUpInterval = (Spinner) mView.findViewById(R.id.spinner_reup_interval);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.row_spn_dropdown, mReupIntervalTitle);
        adapter.setDropDownViewResource(R.layout.row_spn_dropdown);
        spnReUpInterval.setAdapter(adapter);

        int nowInterval = mMeta.getReUpInterval();
        for (int i = 0; i < mReupIntervalTitle.length; i++) {
            if (mReupIntervalTitle[i].startsWith(nowInterval + " ")) {
                spnReUpInterval.setSelection(i);
                break;
            }
        }

        spnReUpInterval.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int interval = Integer.parseInt(mReupIntervalTitle[position].substring(0, 2).trim());
                mMeta.setReUpInterval(interval);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });

        return mView;
    }

    View.OnClickListener mTimezoneListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final Calendar now = (Calendar) v.getTag();
            TimePickerDialog.Builder builder = new TimePickerDialog.Builder(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)) {
                @Override
                public void onPositiveActionClicked(DialogFragment fragment) {
                    TimePickerDialog dialog = (TimePickerDialog) fragment.getDialog();
                    int hour = dialog.getHour();
                    int min = dialog.getMinute();
                    LOG.i(TAG, "save - " + hour + " : " + min);

                    now.set(Calendar.HOUR_OF_DAY, hour);
                    now.set(Calendar.MINUTE, min);

                    switch (v.getId()) {
                        case R.id.tv_timezone_from_ampm:
                        case R.id.tv_timezone_from_time: {
                            tvFromAmPm.setText(now.get(Calendar.AM_PM) == Calendar.AM ? R.string.label_am : R.string.label_pm);
                            tvFromTime.setText(getTimeString(now));
                            mAlarmMgr.setAlarmTimezoneFrom(hour, min);
                            break;
                        }
                        case R.id.tv_timezone_to_ampm:
                        case R.id.tv_timezone_to_time: {
                            tvToAmPm.setText(now.get(Calendar.AM_PM) == Calendar.AM ? R.string.label_am : R.string.label_pm);
                            tvToTime.setText(getTimeString(now));
                            mAlarmMgr.setAlarmTimezoneTo(hour, min);
                            break;
                        }
                    }
                    v.setTag(now);

                    super.onPositiveActionClicked(fragment);
                }

                @Override
                public void onNegativeActionClicked(DialogFragment fragment) {
                    LOG.i(TAG, "Cancelled");
                    super.onNegativeActionClicked(fragment);
                }
            };

            builder.positiveAction("확인")
                    .negativeAction("취소");
            DialogFragment fragment = DialogFragment.newInstance(builder);
            fragment.show(SettingFragment.this.getActivity().getSupportFragmentManager(), null);
        }
    };

    private String getTimeString(Calendar cal) {
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        return (hour > 12 ? String.format("%02d", hour - 12) : String.format("%02d", hour)) + ":" + String.format("%02d", min);
    }

    @Override
    public void refreshView() {

    }
}

