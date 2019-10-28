package info.nightscout.androidaps.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;

/**
 * Created by mike on 14.02.2017.
 */

public class PasswordProtection {

    private static long lastTimePasswordOK = 0;
    private static long millisCachedPassword = 1000*60*(SP.getLong(R.string.key_password_cache_time, (long) 0));//(5*60*1000); //tras acertar la contraseña, no la pedimos durante los siguientes minutos

    static public boolean isLocked(String preference) {
        final String password = SP.getString(preference, "");
        if (password.equals("")) {
            return false;
        }
        return true;
    }

    static public void QueryPassword(final Context context, int stringID, String preference, final Runnable ok, final Runnable fail) {
        final String password = SP.getString(preference, "");
        boolean passwordCached = false;
        millisCachedPassword = 1000*60*(SP.getLong(R.string.key_password_cache_time, (long) 0));//(5*60*1000); //tras acertar la contraseña, no la pedimos durante los siguientes minutos
        if ((lastTimePasswordOK + millisCachedPassword) > System.currentTimeMillis() )
        {
            //Si hace poco tiempo que hemos obtenido la contaseña correcta, no volvemos a pedirla
            passwordCached = true;
            Toast.makeText(context, "Password cached", Toast.LENGTH_SHORT);
        }
        if (password.equals("") || passwordCached) {
            if (ok != null) ok.run();
            return;
        }
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.passwordprompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final TextView label = (TextView) promptsView.findViewById(R.id.passwordprompt_text);
        label.setText(MainApp.gs(stringID));
        final EditText userInput = (EditText) promptsView.findViewById(R.id.passwordprompt_pass);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                String enteredPassword = userInput.getText().toString();
                                if (password.equals(enteredPassword)) {
                                    lastTimePasswordOK = System.currentTimeMillis();
                                    if (ok != null) ok.run();
                                } else {
                                    ToastUtils.showToastInUiThread(context, MainApp.gs(R.string.wrongpassword));
                                    if (fail != null) fail.run();
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
