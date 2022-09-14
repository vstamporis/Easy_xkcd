/**
 * *******************************************************************************
 * Copyright 2016 Tom Praschan
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ******************************************************************************
 */

package de.tap.easy_xkcd.services;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import android.os.Build;
import android.util.Log;

import com.tap.xkcd_reader.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.tap.easy_xkcd.utils.PrefHelper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class ArticleDownloadService extends IntentService {

    private static final String OFFLINE_WHATIF_PATH = "/what if/";
    private static final String OFFLINE_WHATIF_OVERVIEW_PATH = "/what if/overview";

    public ArticleDownloadService() {
        super("ArticleDownloadService");
    }

    NotificationCompat.Builder getNotificationBuilder(String channel) {
        return new NotificationCompat.Builder(this, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setProgress(100, 0, false)
                .setContentTitle(getResources().getString(R.string.loading_offline))
                .setOngoing(true)
                .setAutoCancel(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel("comic");
            notificationManager.deleteNotificationChannel("download");
            notificationManager.createNotificationChannel(new NotificationChannel("comic", getResources().getString(R.string.notification_channel_comic), NotificationManager.IMPORTANCE_HIGH));
            notificationManager.createNotificationChannel(new NotificationChannel("download", getResources().getString(R.string.notification_channel_download), NotificationManager.IMPORTANCE_LOW));
        }
        notificationManager.notify(1, getNotificationBuilder("comic").build());

        PrefHelper prefHelper = new PrefHelper(getApplicationContext());
        File sdCard = prefHelper.getOfflinePath(getApplicationContext());
        File dir = new File(sdCard.getAbsolutePath() + OFFLINE_WHATIF_OVERVIEW_PATH);
        OkHttpClient client = new OkHttpClient();
        Document doc;
        if (!dir.exists()) dir.mkdirs();
        //download overview
//        if (!BuildConfig.DEBUG) {
            try {
                doc = Jsoup.connect("https://what-if.xkcd.com/archive/")
                        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.19 Safari/537.36")
                        .get();
                StringBuilder sb = new StringBuilder();
                Elements titles = doc.select("h1");
                prefHelper.setNewestWhatif(titles.size());

                sb.append(titles.first().text());
                titles.remove(0);
                for (Element title : titles) {
                    sb.append("&&");
                    sb.append(title.text());
                }
                prefHelper.setWhatIfTitles(sb.toString());

                Elements img = doc.select("img.archive-image");
                int count = 1;
                for (Element image : img) {
                    String url = image.absUrl("src");
                    try {
                        Request request = new Request.Builder()
                                .url(url)
                                .build();
                        Response response = client.newCall(request).execute();
                        File file = new File(dir, String.valueOf(count) + ".png");
                        BufferedSink sink = Okio.buffer(Okio.sink(file));
                        sink.writeAll(response.body().source());
                        sink.close();
                        response.body().close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int p = (int) (count / ((float) img.size()) * 100);
                    NotificationCompat.Builder builder = getNotificationBuilder("download");
                    builder.setProgress(100, p, false);
                    notificationManager.notify(1, builder.build());
                    count++;
                }
                if (prefHelper.getNewestWhatIf() == 0)
                    prefHelper.setNewestWhatif(count - 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //download html
            int size = prefHelper.getNewestWhatIf();
            for (int i = 1; i <= size; i++) {
                try {
                    doc = Jsoup.connect("https://what-if.xkcd.com/" + String.valueOf(i)).get();
                    dir = new File(sdCard.getAbsolutePath() + OFFLINE_WHATIF_PATH + String.valueOf(i));
                    dir.mkdirs();
                    File file = new File(dir, String.valueOf(i) + ".html");
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write(doc.outerHtml());
                    writer.close();
                    //download images
                    int count = 1;
                    for (Element e : doc.select(".illustration")) {
                        try {
                            String url = "https://what-if.xkcd.com" + e.attr("src");
                            Request request = new Request.Builder()
                                    .url(url)
                                    .build();
                            Response response = client.newCall(request).execute();
                            dir = new File(sdCard.getAbsolutePath() + OFFLINE_WHATIF_PATH + String.valueOf(i));
                            if (!dir.exists()) dir.mkdirs();
                            file = new File(dir, String.valueOf(count) + ".png");
                            BufferedSink sink = Okio.buffer(Okio.sink(file));
                            sink.writeAll(response.body().source());
                            sink.close();
                            response.body().close();
                            count++;
                        } catch (Exception e2) {
                            Log.e("article" + i, e2.getMessage());
                        }
                    }
                    int p = (int) (i / ((float) size) * 100);
                    NotificationCompat.Builder builder = getNotificationBuilder("download");
                    builder.setProgress(100, p, false);
                    builder.setContentText(i + "/" + size);
                    notificationManager.notify(1, builder.build());
                } catch (Exception e) {
                    Log.e("article" + i, e.getMessage());
                }
            }
//        }
        prefHelper.setSunbeamLoaded();

        Intent restart = new Intent("de.tap.easy_xkcd.ACTION_COMIC");
        restart.putExtra("number", prefHelper.getLastComic());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, restart, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = getNotificationBuilder("comic");
        builder.setContentIntent(pendingIntent)
                .setContentText(getResources().getString(R.string.not_restart));
        notificationManager.notify(1, builder.build());
    }

}
