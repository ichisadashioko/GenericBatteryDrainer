package com.lohjason.genericbatterydrainer.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lohjason.genericbatterydrainer.R;
import com.lohjason.genericbatterydrainer.utils.SharedPrefsUtils;

/**
 * SettingsDialogFragment
 * Created by jason on 9/7/18.
 */
public class SettingsDialogFragment extends BottomSheetDialogFragment {

    public static final  float MAX_AVAILABLE_TEMP  = 55;
    public static final  float MAX_SAFE_TEMP       = 50;
    public static final  float CHARGING_TEMP_LIMIT = 45;
    private static final float MIN_TEMP            = 40;
    private SettingsChangedListener listener;

    TextView     tvTempValue;
    TextView     tvLevelValue;
    SeekBar      seekBarTemp;
    SeekBar      seekBarLevel;
    SwitchCompat switchUseFahrenheit;
    TextView     tvCloseSettings;


    public static SettingsDialogFragment getNewInstance(SettingsChangedListener listener) {
        SettingsDialogFragment fragment = new SettingsDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_settings_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final BottomSheetDialog bottomSheetDialog =
                (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        bottomSheetDialog.setOnShowListener(dialog -> {
            FrameLayout bottomSheet =
                    bottomSheetDialog.findViewById(android.support.design.R.id.design_bottom_sheet);

            if (null != bottomSheet) {
                BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(false);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return bottomSheetDialog;
    }

    private void setupViews(View view) {
        tvTempValue = view.findViewById(R.id.tv_settings_battery_temp_seekbar_value);
        tvLevelValue = view.findViewById(R.id.tv_settings_battery_level_seekbar_value);
        seekBarTemp = view.findViewById(R.id.seekbar_settings_battery_temp);
        seekBarLevel = view.findViewById(R.id.seekbar_settings_battery_level);
        switchUseFahrenheit = view.findViewById(R.id.switch_use_fahrenheit);
        tvCloseSettings = view.findViewById(R.id.tv_close_settings);

        if (SharedPrefsUtils.getUsesFahrenheit(requireContext())) {
            switchUseFahrenheit.setChecked(true);
        }

        seekBarTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = convertProgressToTemp(progress);
                if (value > MAX_SAFE_TEMP) {
                    tvTempValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.material_red_600));
                } else if(value > CHARGING_TEMP_LIMIT){
                    tvTempValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.material_orange_600));
                } else {
                    tvTempValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.material_green_600));
                }
                String unit = "°C";

                if (SharedPrefsUtils.getUsesFahrenheit(requireContext())) {
                    value = (int) Math.round(value * 1.8 + 32);
                    unit = "°F";
                }
                String labelValue = value + unit;
                tvTempValue.setText(labelValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        seekBarLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String labelLevel = progress + "%";
                tvLevelValue.setText(labelLevel);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float targetLevel = (float)seekBarLevel.getProgress();
                float currentLevel = (((MainActivity)requireActivity()).lastBatteryLevel);
                if(targetLevel >= currentLevel){
                    float newTarget = currentLevel > 1 ? currentLevel - 1 : 0;
                    seekBarLevel.setProgress((int)newTarget);
                }
                saveSettings();
            }
        });
        switchUseFahrenheit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.appSettingsChanged(isChecked);
        });
        tvCloseSettings.setOnClickListener(v -> dismiss());

        initLimits();
    }

    private void initLimits() {
        int tempLimit  = SharedPrefsUtils.getTempLimit(requireContext());
        int levelLimit = SharedPrefsUtils.getLevelLimit(requireContext());
        seekBarTemp.setProgress(convertTempToProgress(tempLimit));
        seekBarLevel.setProgress(levelLimit);
        String levelString = levelLimit + "%";
        tvLevelValue.setText(levelString);
        String tempString;

        if (SharedPrefsUtils.getUsesFahrenheit(requireContext())) {
            tempString = Math.round(tempLimit * 1.8 + 32) + "°F";
        } else {
            tempString = Math.round(tempLimit) + "°C";
        }
        tvTempValue.setText(tempString);
    }

    private void saveSettings() {
        SharedPrefsUtils.setLevelLimit(requireContext(), seekBarLevel.getProgress());
        SharedPrefsUtils.setTempLimit(requireContext(), convertProgressToTemp(seekBarTemp.getProgress()));
        SharedPrefsUtils.setUsesFahrenheit(requireContext(), switchUseFahrenheit.isChecked());
        listener.appSettingsChanged(switchUseFahrenheit.isChecked());
    }

    private int convertProgressToTemp(int progress) {
        float scale      = 100f / (MAX_AVAILABLE_TEMP - MIN_TEMP);
        float floatValue = (progress / scale) + MIN_TEMP;
        return Math.round(floatValue);
    }

    private int convertTempToProgress(int temp) {
        float scale = 100f / (MAX_AVAILABLE_TEMP - MIN_TEMP);
        return Math.round((temp - MIN_TEMP) * scale);
    }

    public interface SettingsChangedListener {
        void appSettingsChanged(boolean usesFahrenheit);
    }
}
