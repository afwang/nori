package pe.moe.nori.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import net.simonvt.numberpicker.NumberPicker;
import pe.moe.nori.R;

public class NumberPickerPreference extends DialogPreference {
  private NumberPicker mNumberPicker;
  private Integer mInitialValue;

  public NumberPickerPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
    mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
    // TODO: this should come from XML maybe?
    mNumberPicker.setMinValue(1);
    mNumberPicker.setMaxValue(15);
    if (mInitialValue != null) mNumberPicker.setValue(mInitialValue);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    super.onClick(dialog, which);
    if (which == DialogInterface.BUTTON_POSITIVE) {
      mInitialValue = mNumberPicker.getValue();
      persistInt(mInitialValue);
      callChangeListener(mInitialValue);
    }
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    super.onSetInitialValue(restorePersistedValue, defaultValue);
    int def = (defaultValue instanceof Number) ? (Integer) defaultValue
        : (defaultValue != null) ? Integer.parseInt(defaultValue.toString()) : 1;
    if (restorePersistedValue) {
      mInitialValue = getPersistedInt(def);
    } else mInitialValue = (Integer) defaultValue;
  }

  public int getValue() {
    return getPersistedInt(-1);
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getInt(index, 1);
  }
}
