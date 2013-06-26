package com.ece596.ucsb.localization;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
 
public class Prefs extends PreferenceActivity {
    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        
        final CheckBoxPreference checkboxPref = (CheckBoxPreference) getPreferenceManager().findPreference("filter");
        
        checkboxPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(newValue instanceof Boolean){
                    MainActivity.useFilter = (Boolean)newValue;
                }
                return true;
            }
        });   
    }
}