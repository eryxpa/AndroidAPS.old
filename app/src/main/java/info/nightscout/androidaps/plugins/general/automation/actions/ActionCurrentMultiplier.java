package info.nightscout.androidaps.plugins.general.automation.actions;

import android.util.Log;
import android.widget.LinearLayout;

import com.google.common.base.Optional;

import org.jcw.JCUtil;
import org.json.JSONException;
import org.json.JSONObject;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.plugins.general.automation.elements.InputDouble;
import info.nightscout.androidaps.plugins.general.automation.elements.InputString;
import info.nightscout.androidaps.plugins.general.automation.elements.LabelWithElement;
import info.nightscout.androidaps.plugins.general.automation.elements.LayoutBuilder;
import info.nightscout.androidaps.plugins.general.smsCommunicator.SmsCommunicatorPlugin;
import info.nightscout.androidaps.queue.Callback;
import info.nightscout.androidaps.utils.JsonHelper;
import info.nightscout.androidaps.utils.SP;
import info.nightscout.androidaps.utils.SafeParse;

public class ActionCurrentMultiplier extends Action {

    public InputString currentm = new InputString();

    @Override
    public int friendlyName() {
        return R.string.changecurrentactiondescription;
    }

    @Override
    public String shortDescription() {
        return MainApp.gs(R.string.changecurrentactionlabel, currentm.getValue());
    }

    @Override
    public void doAction(Callback callback) {
        Double currentMulti = SafeParse.stringToDouble(currentm.getValue());
        String key = "openapsama_current_basal_safety_multiplier";
        //Cambiamos el valor de la preferencia en AAPS
        String msg = "JCW. Actualizo SharedPreferences de key " + key + " a valor " + currentMulti + " por AUTOMATION";
        Log.i("JCW", msg);
        SP.putString(key, currentMulti.toString());
        JCUtil.sendTelegramNotification(msg);
    }

    @Override
    public Optional<Integer> icon() {
        return Optional.of(R.drawable.ic_notifications);
    }

    @Override
    public String toJSON() {
        JSONObject o = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            data.put("currentm", currentm.getValue());
            o.put("type", this.getClass().getName());
            o.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return o.toString();
    }

    @Override
    public Action fromJSON(String data) {
        try {
            JSONObject o = new JSONObject(data);
            currentm.setValue(JsonHelper.safeGetString(o, "currentm"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    @Override
    public void generateDialog(LinearLayout root) {

        new LayoutBuilder()
                .add(new LabelWithElement(MainApp.gs(R.string.changecurrentactionvalue), "", currentm))
                .build(root);
    }

}
