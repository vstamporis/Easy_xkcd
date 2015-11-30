package de.tap.easy_xkcd.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.tap.xkcd_reader.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.tap.easy_xkcd.Activities.MainActivity;
import de.tap.easy_xkcd.utils.Comic;
import de.tap.easy_xkcd.utils.Favorites;
import de.tap.easy_xkcd.utils.PrefHelper;

public class OverviewListFragment extends android.support.v4.app.Fragment {
    private static String[] titles;
    private ListAdapter adapter;
    @Bind(R.id.list)
    ListView list;
    private PrefHelper prefHelper;
    private static final String BROWSER_TAG = "browser";
    private static final String OVERVIEW_TAG = "overview";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.overview_list, container, false);
        prefHelper = ((MainActivity) getActivity()).getPrefHelper();
        ButterKnife.bind(this, v);
        list.setFastScrollEnabled(true);
        setHasOptionsMenu(true);

        if (savedInstanceState == null) {
            new updateDatabase().execute();
        } else {
            adapter = new ListAdapter();
            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    showComic(i);
                }
            });
        }
        return v;
    }

    public void showComic(final int pos) {
        android.support.v4.app.FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        android.support.v4.app.Fragment fragment = fragmentManager.findFragmentByTag(BROWSER_TAG);
        if (!prefHelper.overviewFav()) {
            if (!prefHelper.fullOfflineEnabled())
                ((ComicBrowserFragment) fragment).scrollTo(adapter.getCount() - pos - 1, false);
            else
                ((OfflineFragment) fragment).scrollTo(adapter.getCount() - pos - 1, false);
        } else {
            int n = Integer.parseInt(Favorites.getFavoriteList(MainActivity.getInstance())[pos]);
            if (!prefHelper.fullOfflineEnabled())
                ((ComicBrowserFragment) fragment).scrollTo(n - 1, false);
            else
                ((OfflineFragment) fragment).scrollTo(n - 1, false);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(OVERVIEW_TAG)).show(fragment).commitAllowingStateLoss();
        } else {
            Transition left = TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.slide_left);
            Transition right = TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.slide_right);

            OverviewListFragment.this.setExitTransition(left);

            fragment.setEnterTransition(right);

            getFragmentManager().beginTransaction()
                    .hide(fragmentManager.findFragmentByTag(OVERVIEW_TAG))
                    .show(fragment)
                    .commit();
        }

        if (prefHelper.subtitleEnabled()) {
            if (!prefHelper.fullOfflineEnabled())
                ((MainActivity) getActivity()).getToolbar().setSubtitle(String.valueOf(ComicBrowserFragment.sLastComicNumber));
            else
                ((MainActivity) getActivity()).getToolbar().setSubtitle(String.valueOf(OfflineFragment.sLastComicNumber));
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                list.setSelection(pos);
            }
        }, 250);
    }

    private class ListAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        public ListAdapter() {
            inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            if (prefHelper.overviewFav())
                return Favorites.getFavoriteList(MainActivity.getInstance()).length;
            return prefHelper.getNewest();
        }

        @Override
        public String getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view == null) {
                holder = new ViewHolder();
                view = inflater.inflate(R.layout.overview_item, parent, false);
                holder.textView = (TextView) view.findViewById(R.id.tv);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            String label;
            if (prefHelper.overviewFav()) {
                int n = Integer.parseInt(Favorites.getFavoriteList(MainActivity.getInstance())[position]);
                label = n + ": " + prefHelper.getTitle(n);
            } else {
                label = String.valueOf(getCount() - position) + " " + titles[getCount() - position - 1];
                if (prefHelper.checkComicRead(getCount() - position)) {
                    if (prefHelper.nightThemeEnabled())
                        holder.textView.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.tertiary_text_light));
                    else
                        holder.textView.setTextColor(ContextCompat.getColor(getActivity(), R.color.Read));
                } else {
                    if (prefHelper.nightThemeEnabled())
                        holder.textView.setTextColor(ContextCompat.getColor(getActivity(), R.color.Read));
                    else
                        holder.textView.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.tertiary_text_light));
                }
            }
            holder.textView.setText(label);
            return view;
        }
    }

    public static class ViewHolder {
        public TextView textView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        for (int i = 0; i < menu.size() - 2; i++)
            menu.getItem(i).setVisible(false);

        MenuItem item = menu.findItem(R.id.action_favorite);
        item.setVisible(true);
        if (!prefHelper.overviewFav())
            item.setIcon(R.drawable.ic_favorite_outline);
        else
            item.setIcon(R.drawable.ic_action_favorite);

        if (prefHelper.hideDonate())
            menu.findItem(R.id.action_donate).setVisible(false);

        menu.findItem(R.id.action_unread).setVisible(true);


        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favorite:
                if (prefHelper.overviewFav())
                    item.setIcon(R.drawable.ic_favorite_outline);
                else
                    item.setIcon(R.drawable.ic_action_favorite);
                prefHelper.setOverviewFav(!prefHelper.overviewFav());
                adapter = new ListAdapter();
                list.setAdapter(adapter);
                break;
            case R.id.action_unread:
                prefHelper.setComicsUnread();
                adapter.notifyDataSetChanged();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void notifyAdapter() {
        adapter.notifyDataSetChanged();
    }

    private class updateDatabase extends AsyncTask<Void, Integer, Void> {
        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setTitle(getResources().getString(R.string.update_database));
            progress.setIndeterminate(false);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (!prefHelper.databaseLoaded()) {
                InputStream is = getResources().openRawResource(R.raw.comic_titles);
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                try {
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    Log.e("error:", e.getMessage());
                }
                prefHelper.setTitles(sb.toString());
                publishProgress(15);
                Log.d("info", "titles loaded");

                is = getResources().openRawResource(R.raw.comic_trans);
                br = new BufferedReader(new InputStreamReader(is));
                sb = new StringBuilder();
                try {
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    Log.e("error:", e.getMessage());
                }
                prefHelper.setTrans(sb.toString());
                publishProgress(30);
                Log.d("info", "trans loaded");

                is = getResources().openRawResource(R.raw.comic_urls);
                br = new BufferedReader(new InputStreamReader(is));
                sb = new StringBuilder();
                try {
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    Log.e("error:", e.getMessage());
                }
                prefHelper.setUrls(sb.toString(), 1579);
                Log.d("info", "urls loaded");
                prefHelper.setDatabaseLoaded();
            }
            publishProgress(50);
            if (prefHelper.isOnline(getActivity())) {
                int newest;
                try {
                    newest = new Comic(0).getComicNumber();
                } catch (IOException e) {
                    newest = prefHelper.getNewest();
                }
                StringBuilder sbTitle = new StringBuilder();
                sbTitle.append(prefHelper.getComicTitles());
                StringBuilder sbTrans = new StringBuilder();
                sbTrans.append(prefHelper.getComicTrans());
                StringBuilder sbUrl = new StringBuilder();
                sbUrl.append(prefHelper.getComicUrls());
                String title;
                String trans;
                String url;
                Comic comic;
                for (int i = prefHelper.getHighestUrls(); i < newest; i++) {
                    try {
                        comic = new Comic(i + 1);
                        title = comic.getComicData()[0];
                        trans = comic.getTranscript();
                        url = comic.getComicData()[2];
                    } catch (IOException e) {
                        title = "";
                        trans = "";
                        url = "";
                    }
                    sbTitle.append("&&");
                    sbTitle.append(title);
                    sbUrl.append("&&");
                    sbUrl.append(url);
                    sbTrans.append("&&");
                    if (!trans.equals("")) {
                        sbTrans.append(trans);
                    } else {
                        sbTrans.append("n.a.");
                    }
                    float x = newest - prefHelper.getHighestUrls();
                    int y = i - prefHelper.getHighestUrls();
                    int p = (int) ((y / x) * 50);
                    publishProgress(p + 50);
                }
                prefHelper.setTitles(sbTitle.toString());
                prefHelper.setTrans(sbTrans.toString());
                prefHelper.setUrls(sbUrl.toString(), newest);
            }
            return null;
        }

        protected void onProgressUpdate(Integer... pro) {
            progress.setProgress(pro[0]);
        }

        @Override
        protected void onPostExecute(Void dummy) {
            titles = prefHelper.getComicTitles().split("&&");
            progress.dismiss();
            adapter = new ListAdapter();
            list.setAdapter(adapter);
            if (prefHelper.fullOfflineEnabled()) {
                list.setSelection(OfflineFragment.sLastComicNumber);
            } else {
                list.setSelection(ComicBrowserFragment.sLastComicNumber - 1);
            }
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    showComic(i);
                }
            });
        }
    }

}
