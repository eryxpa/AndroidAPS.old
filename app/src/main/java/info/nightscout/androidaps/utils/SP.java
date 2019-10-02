package info.nightscout.androidaps.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jcw.JCUtil;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;

/**
 * Created by mike on 17.02.2017.
 */

public class SP {
    static SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());

    static public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    static public boolean contains(int resourceId) {
        return sharedPreferences.contains(MainApp.gs(resourceId));
    }

    static public String getString(int resourceID, String defaultValue) {
        return sharedPreferences.getString(MainApp.gs(resourceID), defaultValue);
    }

    static public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    static public boolean getBoolean(int resourceID, Boolean defaultValue) {
        try {
            return sharedPreferences.getBoolean(MainApp.gs(resourceID), defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    static public boolean getBoolean(String key, Boolean defaultValue) {
        try {
            return sharedPreferences.getBoolean(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    static public Double getDouble(int resourceID, Double defaultValue) {
        Double valor = SafeParse.stringToDouble(sharedPreferences.getString(MainApp.gs(resourceID), defaultValue.toString()));
        Double valorCustom = valor;

        //Adaptación JCW para opcionalmente recuperar el multiplicador de un fichero gestionado externamente
        if (resourceID == R.string.key_openapsama_current_basal_safety_multiplier)
        {
            valorCustom = JCUtil.getCustom_current_basal_safety_multiplier(valor);
            JCUtil.renameCustom_current_basal_safety_multiplier();
        }
        //Adaptación JCW para opcionalmente recuperar el multiplicador de un fichero gestionado externamente
        if (resourceID == R.string.key_openapsama_max_daily_safety_multiplier)
        {
            valorCustom = JCUtil.getCustom_max_daily_safety_multiplier(valor);
            JCUtil.renameCustom_max_daily_safety_multiplier();
        }

        if (valor.equals(valorCustom) == false)
        {
            //Si el custom es distinto, cambiamos el valor de la preferencia en AAPS
            String msg = "JCW. Actualizo SharedPreferences de resourceID " + resourceID + " ("+MainApp.gs(resourceID)+") de valor " + valor + " a valor " + valorCustom;
            Log.i("JCW", msg);
            putString(resourceID, valorCustom.toString());
            JCUtil.sendTelegramNotification(msg);
        }

        valor = valorCustom;


        return valor;
    }

    static public Double getDouble(String key, Double defaultValue) {
        Double valor = SafeParse.stringToDouble(sharedPreferences.getString(key, defaultValue.toString()));
        Double valorCustom = valor;

        //Adaptación JCW para opcionalmente recuperar el multiplicador de un fichero gestionado externamente
        if ("openapsama_current_basal_safety_multiplier".equals(key))
        {
            valorCustom = JCUtil.getCustom_current_basal_safety_multiplier(valor);
            JCUtil.renameCustom_current_basal_safety_multiplier();
        }
        //Adaptación JCW para opcionalmente recuperar el multiplicador de un fichero gestionado externamente
        if ("openapsama_max_daily_safety_multiplier".equals(key))
        {
            valorCustom = JCUtil.getCustom_max_daily_safety_multiplier(valor);
            JCUtil.renameCustom_max_daily_safety_multiplier();
        }

        if (valor.equals(valorCustom) == false)
        {
            //Si el custom es distinto, cambiamos el valor de la preferencia en AAPS
            String msg = "JCW. Actualizo SharedPreferences de key " + key + " de valor " + valor + " a valor " + valorCustom;
            Log.i("JCW", msg);
            putString(key, valorCustom.toString());
            JCUtil.sendTelegramNotification(msg);
        }

        valor = valorCustom;
        return valor;
    }

    static public int getInt(int resourceID, Integer defaultValue) {
        try {
            return sharedPreferences.getInt(MainApp.gs(resourceID), defaultValue);
        } catch (Exception e) {
            return SafeParse.stringToInt(sharedPreferences.getString(MainApp.gs(resourceID), defaultValue.toString()));
        }
    }

    static public int getInt(String key, Integer defaultValue) {
        try {
            return sharedPreferences.getInt(key, defaultValue);
        } catch (Exception e) {
            return SafeParse.stringToInt(sharedPreferences.getString(key, defaultValue.toString()));
        }
    }

    static public long getLong(int resourceID, Long defaultValue) {
        try {
            return sharedPreferences.getLong(MainApp.gs(resourceID), defaultValue);
        } catch (Exception e) {
            return SafeParse.stringToLong(sharedPreferences.getString(MainApp.gs(resourceID), defaultValue.toString()));
        }
    }

    static public long getLong(String key, Long defaultValue) {
        try {
            return sharedPreferences.getLong(key, defaultValue);
        } catch (Exception e) {
            return SafeParse.stringToLong(sharedPreferences.getString(key, defaultValue.toString()));
        }
    }

    static public void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    static public void putBoolean(int resourceID, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(MainApp.gs(resourceID), value);
        editor.apply();
    }

    static public void putDouble(String key, double value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, Double.toString(value));
        editor.apply();
    }

    static public void putLong(String key, long value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    static public void putLong(int resourceID, long value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(MainApp.gs(resourceID), value);
        editor.apply();
    }

    static public void putInt(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    static public void putInt(int resourceID, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(MainApp.gs(resourceID), value);
        editor.apply();
    }

    static public void incInt(int resourceID) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int value = SP.getInt(resourceID, 0) + 1;
        editor.putInt(MainApp.gs(resourceID), value);
        editor.apply();
    }

    static public void putString(int resourceID, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MainApp.gs(resourceID), value);
        editor.apply();
    }

    static public void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    static public void remove(int resourceID) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(MainApp.gs(resourceID));
        editor.apply();
    }

    static public void remove(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }
}
