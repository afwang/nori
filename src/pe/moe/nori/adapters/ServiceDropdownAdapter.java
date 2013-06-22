package pe.moe.nori.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;
import pe.moe.nori.R;
import pe.moe.nori.providers.ServiceSettingsProvider;

import java.util.List;

/**
 * An adapter used for the service picker {@link android.widget.Spinner} in {@link pe.moe.nori.SearchActivity}.
 */
public class ServiceDropdownAdapter extends ArrayAdapter<ServiceSettingsProvider.ServiceSettings> {

  public ServiceDropdownAdapter(Context context, List<ServiceSettingsProvider.ServiceSettings> objects) {
    super(context, R.layout.sherlock_spinner_dropdown_item, android.R.id.text1, objects);
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).id;
  }

  public int getPositionByItemId(long itemId) {
    for (int i = 0; i < getCount(); i++) {
      if (getItemId(i) == itemId) {
        return i;
      }
    }

    // Default to first item.
    return getCount() > 0 ? 0 : -1;
  }
}
