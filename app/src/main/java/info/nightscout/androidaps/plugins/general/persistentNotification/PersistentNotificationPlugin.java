package info.nightscout.androidaps.plugins.general.persistentNotification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.core.app.TaskStackBuilder;

import com.squareup.otto.Subscribe;

import javax.annotation.Nonnull;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainActivity;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.IobTotal;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.db.DatabaseHelper;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.events.EventExtendedBolusChange;
import info.nightscout.androidaps.events.EventInitializationChanged;
import info.nightscout.androidaps.events.EventNewBasalProfile;
import info.nightscout.androidaps.events.EventPreferenceChange;
import info.nightscout.androidaps.events.EventRefreshOverview;
import info.nightscout.androidaps.events.EventTempBasalChange;
import info.nightscout.androidaps.events.EventTreatmentChange;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PluginDescription;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.configBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.GlucoseStatus;
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.IobCobCalculatorPlugin;
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.events.EventAutosensCalculationFinished;
import info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin;
import info.nightscout.androidaps.utils.DecimalFormatter;

/**
 * Created by adrian on 23/12/16.
 */

public class PersistentNotificationPlugin extends PluginBase {

    private static PersistentNotificationPlugin plugin;
    private Notification notification;

    public static PersistentNotificationPlugin getPlugin() {
        if (plugin == null) plugin = new PersistentNotificationPlugin(MainApp.instance());
        return plugin;
    }

    public static final String CHANNEL_ID = "AndroidAPS-Ongoing";

    public static final int ONGOING_NOTIFICATION_ID = 4711;
    private final Context ctx;

    /// For Android Auto
    /// Intents are not declared in manifest and not consumed, this is intentionally because actually we can't do anything with
    private static final String PACKAGE = "info.nightscout";
    private static final String READ_ACTION =
            "info.nightscout.androidaps.ACTION_MESSAGE_READ";
    private static final String REPLY_ACTION =
            "info.nightscout.androidaps.ACTION_MESSAGE_REPLY";
    private static final String CONVERSATION_ID = "conversation_id";
    private static final String EXTRA_VOICE_REPLY = "extra_voice_reply";
    /// End Android Auto


    private PersistentNotificationPlugin(Context ctx) {
        super(new PluginDescription()
                .mainType(PluginType.GENERAL)
                .neverVisible(true)
                .pluginName(R.string.ongoingnotificaction)
                .enableByDefault(true)
                .alwaysEnabled(true)
                .showInList(false)
                .description(R.string.description_persistent_notification)
        );
        this.ctx = ctx;
    }

    @Override
    protected void onStart() {
        createNotificationChannel(); // make sure channels exist before triggering updates through the bus
        MainApp.bus().register(this);
        triggerNotificationUpdate();
        super.onStart();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager mNotificationManager =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            @SuppressLint("WrongConstant") NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onStop() {
        MainApp.bus().unregister(this);
        MainApp.instance().stopService(new Intent(MainApp.instance(), DummyService.class));
    }

    private void triggerNotificationUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            MainApp.instance().startForegroundService(new Intent(MainApp.instance(), DummyService.class));
        else
            MainApp.instance().startService(new Intent(MainApp.instance(), DummyService.class));
    }

    @Nonnull
    public Notification updateNotification() {
        String line1 = null;
        String line2 = null;
        String line3 = null;
        NotificationCompat.CarExtender.UnreadConversation.Builder unreadConversationBuilder = null;

        if (ConfigBuilderPlugin.getPlugin().getActiveProfileInterface() != null && ProfileFunctions.getInstance().isProfileValid("Notification")) {
            String line1_aa;
            String units = ProfileFunctions.getInstance().getProfileUnits();


            BgReading lastBG = DatabaseHelper.lastBg();
            GlucoseStatus glucoseStatus = GlucoseStatus.getGlucoseStatusData();

            if (lastBG != null) {
                line1 = line1_aa = lastBG.valueToUnitsToString(units);
                if (glucoseStatus != null) {
                    line1 += "  Δ" + deltastring(glucoseStatus.delta, glucoseStatus.delta * Constants.MGDL_TO_MMOLL, units)
                            + " avgΔ" + deltastring(glucoseStatus.avgdelta, glucoseStatus.avgdelta * Constants.MGDL_TO_MMOLL, units);
                    line1_aa += "  " + lastBG.directionToSymbol();
                } else {
                    line1 += " " +
                            MainApp.gs(R.string.old_data) +
                            " ";
                    line1_aa += line1 + ".";
                }
            } else {
                line1 = line1_aa = MainApp.gs(R.string.missed_bg_readings);
            }

            TemporaryBasal activeTemp = TreatmentsPlugin.getPlugin().getTempBasalFromHistory(System.currentTimeMillis());
            if (activeTemp != null) {
                line1 += "  " + activeTemp.toStringShort();
                line1_aa += "  " + activeTemp.toStringShort() + ".";
            }

            //IOB
            TreatmentsPlugin.getPlugin().updateTotalIOBTreatments();
            TreatmentsPlugin.getPlugin().updateTotalIOBTempBasals();
            IobTotal bolusIob = TreatmentsPlugin.getPlugin().getLastCalculationTreatments().round();
            IobTotal basalIob = TreatmentsPlugin.getPlugin().getLastCalculationTempBasals().round();


            line2 = MainApp.gs(R.string.treatments_iob_label_string) + " " + DecimalFormatter.to2Decimal(bolusIob.iob + basalIob.basaliob) + "U " + MainApp.gs(R.string.cob) + ": " + IobCobCalculatorPlugin.getPlugin().getCobInfo(false, "PersistentNotificationPlugin").generateCOBString();
            String line2_aa = MainApp.gs(R.string.treatments_iob_label_string) + " " + DecimalFormatter.to2Decimal(bolusIob.iob + basalIob.basaliob) + "U. " + MainApp.gs(R.string.cob) + ": " + IobCobCalculatorPlugin.getPlugin().getCobInfo(false, "PersistentNotificationPlugin").generateCOBString() + ".";


            line3 = DecimalFormatter.to2Decimal(ConfigBuilderPlugin.getPlugin().getActivePump().getBaseBasalRate()) + " U/h";
            String line3_aa = DecimalFormatter.to2Decimal(ConfigBuilderPlugin.getPlugin().getActivePump().getBaseBasalRate()) + " U/h.";


            line3 += " - " + ProfileFunctions.getInstance().getProfileName();
            line3_aa += " - " + ProfileFunctions.getInstance().getProfileName() + ".";

            /// For Android Auto
            Intent msgReadIntent = new Intent()
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .setAction(READ_ACTION)
                    .putExtra(CONVERSATION_ID, ONGOING_NOTIFICATION_ID)
                    .setPackage(PACKAGE);

            PendingIntent msgReadPendingIntent =
                    PendingIntent.getBroadcast(ctx,
                            ONGOING_NOTIFICATION_ID,
                            msgReadIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

            Intent msgReplyIntent = new Intent()
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .setAction(REPLY_ACTION)
                    .putExtra(CONVERSATION_ID, ONGOING_NOTIFICATION_ID)
                    .setPackage(PACKAGE);

            PendingIntent msgReplyPendingIntent = PendingIntent.getBroadcast(
                    ctx,
                    ONGOING_NOTIFICATION_ID,
                    msgReplyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Build a RemoteInput for receiving voice input from devices
            RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY).build();

            // Create the UnreadConversation
            unreadConversationBuilder =
                    new NotificationCompat.CarExtender.UnreadConversation.Builder(line1_aa + "\n" + line2_aa)
                            .setLatestTimestamp(System.currentTimeMillis())
                            .setReadPendingIntent(msgReadPendingIntent)
                            .setReplyAction(msgReplyPendingIntent, remoteInput);

            /// Add dot to produce a "more natural sounding result"
            unreadConversationBuilder.addMessage(line3_aa);
            /// End Android Auto
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID);
        builder.setOngoing(true);
        builder.setOnlyAlertOnce(true);
        builder.setCategory(NotificationCompat.CATEGORY_STATUS);
        builder.setSmallIcon(MainApp.getNotificationIcon());
        Bitmap largeIcon = BitmapFactory.decodeResource(ctx.getResources(), MainApp.getIcon());
        builder.setLargeIcon(largeIcon);
        builder.setContentTitle(line1 != null ? line1 : MainApp.gs(R.string.noprofileset));
        builder.setContentText(line2 != null ? line2 : MainApp.gs(R.string.noprofileset));
        builder.setSubText(line3 != null ? line3 : MainApp.gs(R.string.noprofileset));
        /// Android Auto
        if (unreadConversationBuilder != null) {
            builder.extend(new NotificationCompat.CarExtender()
                    .setUnreadConversation(unreadConversationBuilder.build()));
        }
        /// End Android Auto


        Intent resultIntent = new Intent(ctx, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(ctx);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        android.app.Notification notification = builder.build();
        mNotificationManager.notify(ONGOING_NOTIFICATION_ID, notification);
        this.notification = notification;
        return notification;
    }

    private String deltastring(double deltaMGDL, double deltaMMOL, String units) {
        String deltastring = "";
        if (deltaMGDL >= 0) {
            deltastring += "+";
        } else {
            deltastring += "-";

        }
        if (units.equals(Constants.MGDL)) {
            deltastring += DecimalFormatter.to1Decimal(Math.abs(deltaMGDL));
        } else {
            deltastring += DecimalFormatter.to1Decimal(Math.abs(deltaMMOL));
        }
        return deltastring;
    }

    /***
     * returns the current ongoing notification.
     *
     * If it does not exist, return a dummy notification. This should only happen if onStart() wasn't called.
     */

    public Notification getLastNotification() {
        if (notification != null) return  notification;
        else return new Notification();
    }


    @Subscribe
    public void onStatusEvent(final EventPreferenceChange ev) {
        triggerNotificationUpdate();
    }

    @Subscribe
    public void onStatusEvent(final EventTreatmentChange ev) {
        triggerNotificationUpdate();
    }

    @Subscribe
    public void onStatusEvent(final EventTempBasalChange ev) {
        triggerNotificationUpdate();
    }

    @Subscribe
    public void onStatusEvent(final EventExtendedBolusChange ev) {
        triggerNotificationUpdate();
    }

    @Subscribe
    public void onStatusEvent(final EventAutosensCalculationFinished ev) {
        triggerNotificationUpdate();
    }

    @Subscribe
    public void onStatusEvent(final EventNewBasalProfile ev) {
        triggerNotificationUpdate();
    }

    @Subscribe
    public void onStatusEvent(final EventInitializationChanged ev) {
        triggerNotificationUpdate();
    }

    @Subscribe
    public void onStatusEvent(final EventRefreshOverview ev) {
        triggerNotificationUpdate();
    }

}
