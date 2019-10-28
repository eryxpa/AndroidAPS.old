package org.jcw;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.plugins.aps.loop.APSResult;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.utils.SP;

public class JCUtil {

    public static final String current_basal_safety_multiplier = "current_basal_safety_multiplier";
    public static final String max_daily_safety_multiplier = "max_daily_safety_multiplier";
    public static final String CUSTOM_FOLDER = "/AndroidAPS-custom/";
    private static File externalStorageFile = null;
    private static String telegram_group_URL = "";


    public static Double getCustom_current_basal_safety_multiplier(double defaultValue)
    {
        return getCustomValueFromFile(current_basal_safety_multiplier, defaultValue);
    }

    public static Double getCustom_max_daily_safety_multiplier(double defaultValue)
    {
        return getCustomValueFromFile(max_daily_safety_multiplier, defaultValue);
    }


    private static double getCustomValueFromFile(String valueName, double defaultValue)
    {
        double custom_value = defaultValue;
        Log.d("JCW", "Se busca customValue para valor "+valueName+ ". Pasado valor por defecto: "+defaultValue);

        try {
            File sd = getExternalStorageFile();
            File customCurrentMultiFile = new File(sd.getCanonicalPath() + CUSTOM_FOLDER + valueName);
            if (customCurrentMultiFile.exists())
            {
                //Existe fichero propio donde guardamos nuestro current_basal_safety_multiplier. Debe tener un double dentro y solo eso
                byte[] content = readFileContent(customCurrentMultiFile.getCanonicalPath());
                try {
                    double valor = Double.parseDouble(new String(content));
                    if (valor > 0 && valor < 10)
                    {
                        Log.i("JCW", "Leido "+valueName+" con valor " + valor + ". Lo utilizaremos");
                        custom_value = valor;
                    }
                    else
                    {
                        //Valor fuera de rangos coherentes
                        Log.e("JCW", "Valor de "+valueName+" fuera de rango. valor " + valor + ". NO se utilizará");
                    }

                }catch (NumberFormatException e)
                {
                    Log.e("JCW", "El fichero "+customCurrentMultiFile.getCanonicalPath()+" no contiene un número double. " + e.getLocalizedMessage());
                }
            }
            else
            {
                Log.d("JCW", "No hay fichero custom " + customCurrentMultiFile.getAbsolutePath());
            }
        }
        catch (Throwable t)
        {
            Log.e("JCW", "Problema al intentar recuperar custom value. " + t.getLocalizedMessage());
        }

        return custom_value;
    }


    public static void renameCustom_current_basal_safety_multiplier()
    {
        renameCustomFile(current_basal_safety_multiplier, null);
    }

    public static void renameCustom_max_daily_safety_multiplier()
    {
        renameCustomFile(max_daily_safety_multiplier, null);
    }



    private static void renameCustomFile(String customFileName, String newName)
    {
        try {
            File sd = getExternalStorageFile();
            File customCurrentMultiFile = new File(sd.getCanonicalPath() + CUSTOM_FOLDER + customFileName);
            //Si existe el fichero, lo renombramos por el nombre pasado por parámetro
            if (customCurrentMultiFile.exists())
            {
                //Existe fichero propio donde guardamos nuestro current_basal_safety_multiplier
                if ("".equals(newName) || newName == null)
                {
                    //Si no nos han pasado nuevo nombre por parámetro, lo renombramos por mismo nombre con sufijo .last
                    newName = customFileName + ".last";
                }
                //Si hay un fichero con el nuevo nombre, lo borramos primero
                File newNameFile = new File(sd.getCanonicalPath() + CUSTOM_FOLDER + newName);
                if (newNameFile.exists())
                {
                    newNameFile.delete();
                }
                //Renombramos fichero
                boolean renamed = customCurrentMultiFile.renameTo(newNameFile);
                if (renamed)
                {
                    sendTelegramNotification("Fichero "+customFileName+ " renombrado a " + newName);
                }
            }
        }
        catch (Throwable t)
        {
            Log.e("JCW", "renameCustomFile: Problema: " + t.getLocalizedMessage());
        }
    }



    static public void sendTelegramSMSResponse(String mensaje)
    {
        sendTelegramMessage(mensaje);
    }


    static public void sendTelegramNotification(String mensaje)
    {
        if (SP.getBoolean(R.string.key_notify_by_telegram, false) && !"".equals(SP.getString(R.string.key_telegram_group_url, "")))
        {
            sendTelegramMessage(mensaje);
        }
    }


    static public void sendTelegramMessage(String mensaje)
    {
        ConnectivityManager connMgr = (ConnectivityManager) MainApp.instance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // Operaciones http
            Log.i("JCW", "OK. Hay conexión para poder enviar mensaje telegram.");
            new NotifyByTelegramTask(mensaje).execute();

        } else {
            // Mostrar errores
            Log.e("JCW", "Sin conexión. No podrá enviarse mensaje telegram");
        }
    }




    public static String dec(double myDouble, int decimals)
    {
        String formatter = "%1$,."+decimals+"f";
        return String.format(formatter, myDouble);
    }

    public static String dec(double myDouble, int decimals, int units)
    {
        String formatter = "%1$,04."+decimals+"f";
        return String.format(formatter, myDouble);
    }

    public static void infoTempTelegram(String command, PumpEnactResult r, double absoluteRate, int durationInMinutes) {
        try
        {
            Log.i("JCW", "adaptación JCW. " + command);
            //informar de basal temporal que se desea aplicar. Aquí aún no sabemos si se aplicará correctamente

            if (r!=null)
            {
                if (r.enacted==true)
                {
                    String extraMsg = "." + " Success: " + r.success + " Enacted: " + r.enacted + " - " + r.toString()+ "\n Command: " + command;
                    String mensaje = "";

                    if (durationInMinutes == 0)
                    {
                        //Es una cancelación de basal temporal
                        mensaje = "Cancelada basal temporal previa. Minutos: " + durationInMinutes + " rate: " + absoluteRate + extraMsg;
                    }
                    else
                    {
                        mensaje = "TBR " + dec(absoluteRate,2) + " ("+r.percent+"%) aplicada por " + durationInMinutes + "m" + extraMsg;

                    }

                    JCUtil.sendTelegramNotification(mensaje);
                }
                else
                {
                    JCUtil.sendTelegramNotification("Problema al aplicar TBR. NO ENACTED: rate: " + absoluteRate + " mins: " + durationInMinutes + " success: " + r.success + " enacted: " + r.enacted + " command: " + command);
                }
            }
            else
            {
                JCUtil.sendTelegramNotification("PumpEnactResult nulo: rate: " + absoluteRate + " mins: " + durationInMinutes + " success: " + r.success + " enacted: " + r.enacted + " command: " + command);
                Log.i("JCW", "adaptación JCW. " + command + ". r es null");
            }
        }
        catch (Throwable t)
        {
            Log.e("JCW", "problema al querer notificar TBR en " + command + ". " + t.getLocalizedMessage());
            t.printStackTrace();
        }
    }

    public static void infoProfileSetTelegram(Profile profile, PumpEnactResult r, String command) {
        try
        {
            Log.i("JCW", "adaptación JCW. " + command);
            //informar de basal temporal que se desea aplicar. Aquí aún no sabemos si se aplicará correctamente

            if (r!=null)
            {
                if (r.enacted==true)
                {
                    String extraMsg = "." + " Success: " + r.success + " Enacted: " + r.enacted + " - " + r.toString()+ "\n Command: " + command;
                    String mensaje = "";

                    mensaje = "CAMBIO de PERFIL realizado. "+dec(profile.baseBasalSum(),2)+"u/dia al "+profile.getPercentage()+"% ("+dec((profile.baseBasalSum()/100*profile.getPercentage()),2)+") \n\nPerfil: \n" + profile.toString(2);
                    JCUtil.sendTelegramNotification(mensaje);
                }
                else
                {
                    JCUtil.sendTelegramNotification("NO se ha cambiado de perfil. success: " + r.success + " enacted: " + r.enacted + " command: " + command);
                }
            }
            else
            {
                JCUtil.sendTelegramNotification("PumpEnactResult nulo: Cambio de perfil pero enactResult es nulo. success: " + r.success + " enacted: " + r.enacted + " command: " + command);
                Log.i("JCW", "adaptación JCW. " + command + ". r es null");
            }
        }
        catch (Throwable t)
        {
            Log.e("JCW", "problema al querer notificar cambio de perfil en " + command + ". " + t.getLocalizedMessage());
            t.printStackTrace();
        }
    }


    static public void notifyTBR(APSResult request) {

        String tbrInfo = "";
        tbrInfo = createTbrInfo(request);

        Log.i("JCW", "Se va a escribir en fichero de tbrs");
        writeToFileTempBasals(tbrInfo);

        Log.i("JCW", "Se va a notificar por telegram");
        sendTelegramNotification(tbrInfo);

    }

    static public void writeToFileTempBasals(String tbrInfo) {
        try {
            File externalStorageF = getExternalStorageFile();

            String rutaFichero = externalStorageF.getCanonicalPath() + "/tbrs.txt";
            Log.i("JCW", "se intenta obtener File contra ruta: "+rutaFichero);

            File file = new File(rutaFichero);
            Log.i("JCW", "obtenido File contra ruta: "+file.getCanonicalPath());

            if (file.exists())
            {
                Log.i("JCW", "existe el fichero "+file.getCanonicalPath());
            }
            else
            {
                Log.i("JCW", "Parece que NO existe el fichero "+file.getCanonicalPath());
                file.createNewFile();
            }

            Log.i("JCW", "Vamos a intentar escribir en fichero "+file.getCanonicalPath() + " el dato "+tbrInfo);

            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            //bw.write(request.toString());
            bw.append(tbrInfo);
            bw.append('\n');
            bw.flush();
            bw.close();
            Log.i("JCW", "Escrito dato al fichero "+file.getCanonicalPath());
//            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
//            outputStreamWriter.write(data);
//            outputStreamWriter.close();
        }
        catch (Exception e) {
            Log.e("JCW", "File write failed: " + e.toString());
        }
    }

    @NotNull
    private static File getExternalStorageFile() throws IOException {

        if (externalStorageFile != null)
        {
            return externalStorageFile;
        }

        Log.i("JCW", "Se va a solicitar ruta a la sd");
        File sdcard = Environment.getExternalStorageDirectory();

        Log.i("JCW", "obtenida ruta al almacenamiento externo: "+sdcard.getCanonicalPath());
        externalStorageFile = sdcard;
        return sdcard;
    }

    public static String fechaHoraLarga(Date fechaHora)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyy HH:mm");
        return sdf.format(fechaHora) + "h";
    }

    private static String createTbrInfo(APSResult request) {
        String result = "";
        try {

            long tbrDate = request.date;
            String tbrDateString = fechaHoraLarga(new Date(tbrDate));
            int tbrDur = request.duration;
            double tbrRate = request.rate;
            int tbrPercent = request.percent;
            String tbrReason = request.reason;

            String tbrAction = "";
            if (tbrDur == 0)
            {
                tbrAction = "TBR cancelar";
            }
            else
            {
                tbrAction = "TBR " + dec(tbrRate, 3) + " ("+tbrPercent+"%) por "+tbrDur + "m";
            }

            result = tbrAction + "  ;  " + tbrReason + " ("+tbrDateString+")";

            Log.i("JCW", "tbrInfo: "+result);
        }
        catch (Exception e)
        {
            Log.i("JCW", "Excepcion creando tbrInfo: "+e.getMessage());
        }
        return ("Prop: " + result);
    }


    static public byte[] readFileContent(String path)
    {
        byte[] content = new byte[0];

        try {
            Log.d("JCW", "Se va a leer el fichero: " + path);
            content = JCFileUtil.readFromFile(path);
            int bytes_readed = 0;
            if (content != null)
            {
                bytes_readed = content.length;
            }
            Log.d("JCW", "Leidos "+bytes_readed+" bytes del fichero: " + path);

        }
        catch (Throwable t)
        {
            Log.e("JCW", "Problema "+t.getLocalizedMessage()+" al leer fichero: " + path);
            t.printStackTrace();
        }

        return content;
    }


    public static void setTelegramURL(String telegramUrlConfigured) {
        if ("".equals(telegramUrlConfigured) || null == telegramUrlConfigured)
        {
            Log.e("JCW", "No se ha configurado URL para grupo de telegram. Está vacía o null en las preferencias del cliente NS");
            return;
        }

        telegram_group_URL = telegramUrlConfigured;
        Log.i("JCW", "Telegram group URL: " + telegram_group_URL);
    }

    public static String getTelegramURL()
    {
        return telegram_group_URL;
    }

}
