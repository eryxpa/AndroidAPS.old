package org.jcw;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NotifyByTelegramTask extends AsyncTask<String, Void, String> {

    private String mensajeAEnviar = "Mensaje";
    private Exception exception;


    public NotifyByTelegramTask(String mensaje) {
        //this.activity = a;
        this.mensajeAEnviar = mensaje;
    }

    protected String doInBackground(String... urls) {
        StringBuffer text = new StringBuffer();
        try {
            Log.i("JCW", "Se va a enviar mensaje por telegram: " + this.mensajeAEnviar);

            String uri = Uri.parse(JCUtil.getTelegramURL())
                    .buildUpon()
                    .appendQueryParameter("text", this.mensajeAEnviar + " [AAPS "+ Build.MANUFACTURER + " " + Build.MODEL+"]")
                    .build().toString();

            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStreamReader in = new InputStreamReader(urlConnection.getInputStream());
                // Acciones a realizar con el flujo de datos
                BufferedReader buff = new BufferedReader(in);
                String line;
                do {
                    line = buff.readLine();
                    text.append(line + "\n");
                } while (line != null);
                Log.i("JCW", "Respuesta a envío de mensaje telegram: " + text.toString());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("JCW", "Problema al enviar telegram: " + e.getLocalizedMessage());
            } finally {
                urlConnection.disconnect();

            }
            ;
        } catch (Exception e) {
            this.exception = e;

            return null;
        } finally {
        }
        return text.toString();

    }

    protected void onPostExecute(String feed) {
        // TODO: check this.exception
        // TODO: do something with the feed
        Log.i("JCW", "invocado onPostExecute para el mensaje telegram: " + this.mensajeAEnviar);
    }
}

