package com.johanpmeert.btcwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gson.*;

/**
 * Implementation of App Widget functionality.
 */
public class NewAppWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        for (int appWidgetId : appWidgetIds) {
            // Since we need to get data from Internet, we use an async task
            // doInBackground will get the data
            // OnPostExecute will update the widget with the result
            new GetRawBitstampData(views, appWidgetId, appWidgetManager).execute();
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    public static class RawBitstampData {
        String high;
        String last;
        String timestamp;
        String bif;
        String vwap;
        String volume;
        String low;
        String ask;
        String open;
    }

    private static class GetRawBitstampData extends AsyncTask<Void, Void, RawBitstampData> {

        private RemoteViews views;
        private int WidgetID;
        private AppWidgetManager WidgetManager;

        public GetRawBitstampData(RemoteViews rViews, int appWidgetID, AppWidgetManager appWidgetManager) {
            this.views = rViews;
            this.WidgetID = appWidgetID;
            this.WidgetManager = appWidgetManager;
        }

        @Override
        protected RawBitstampData doInBackground(Void... Voids) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                final URL url = new URL("https://www.bitstamp.net/api/v2/ticker/btcusd/");
                //Log.e("Opening connection", url.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                //Log.e("Returned JSON", buffer.toString());
                Gson gson = new GsonBuilder().setLenient().create();  // take care of "malformed JSON" error
                RawBitstampData rawBitstampData = gson.fromJson(buffer.toString().trim(), RawBitstampData.class);
                //Log.e("rawBitstampData.last", rawBitstampData.last);
                return rawBitstampData;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(RawBitstampData rbData) {
            String widgetText;
            String lowText = "";
            String highText = "";
            String time = "";
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            TimeZone tz = TimeZone.getTimeZone("Europe/Brussels");
            sdf.setTimeZone(tz);
            DecimalFormat df = new DecimalFormat("#,###");
            if (rbData != null) {
                widgetText = df.format(new BigDecimal(rbData.last));
                lowText = df.format(new BigDecimal(rbData.low));
                highText = df.format(new BigDecimal(rbData.high));
                time = sdf.format(new Date(Long.parseLong(rbData.timestamp) * 1000));
                Log.e("Time", time);
            } else widgetText = "loading";

            // Construct the RemoteViews object
            views.setTextViewText(R.id.appwidget_text, "$" + widgetText);
            views.setTextViewText(R.id.textView, "L: " + lowText + "  H: " + highText);
            views.setTextViewText(R.id.textView2, "BTC/USD @ " + time);

            // Instruct the widget manager to update the widget
            WidgetManager.updateAppWidget(WidgetID, views);
        }
    }

}

