package kr.azazel.barcode.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;

import kr.azazel.barcode.R;
import kr.azazel.barcode.local.AzAppDataHelper;
import kr.azazel.barcode.service.BackupRestoreUtil;

public class MySettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "MySettingsFragment";

    private AzAppDataHelper dataHelper;
    private ObjectMapper obejctMapper = new ObjectMapper();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        dataHelper = AzAppDataHelper.getInstance();

//        if ("KR".equals(Locale.getDefault().getCountry())) {
//            findPreference("lab").setVisible(true);
//        }

        findPreference("share_file").setOnPreferenceClickListener(preference -> {
            BackupRestoreUtil.backupFile(preference.getContext());
            return true;
        });

        findPreference("joongo").setOnPreferenceClickListener(preference -> {
            BackupRestoreUtil.backupFile(preference.getContext());
            return true;
        });
    }
}
