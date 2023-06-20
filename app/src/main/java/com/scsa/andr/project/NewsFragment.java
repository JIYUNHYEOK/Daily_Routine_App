package com.scsa.andr.project;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.scsa.andr.project.databinding.RowBinding;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsFragment extends Fragment implements TextToSpeech.OnInitListener {
    private static final String TAG = "NewsFragment";
    private ListView listView;
    private String rssUrl;
    private String newsTitle;
    private TextToSpeech tts;

    public NewsFragment() {
        // Required empty public constructor
    }

    public static NewsFragment newInstance(String newsTitle, String rssUrl) {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();
        args.putString("title", newsTitle);
        args.putString("url", rssUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            newsTitle = getArguments().getString("title");
            rssUrl = getArguments().getString("url");
        }
        tts = new TextToSpeech(getContext(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        listView = view.findViewById(R.id.listView);

        new MyAsyncTask().execute(rssUrl);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getContext(), "TTS language is not supported.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "TTS initialization failed.", Toast.LENGTH_SHORT).show();
        }
    }

    class MyAsyncTask extends AsyncTask<String, String, List<HaniItem>> {
        List<HaniItem> list = new ArrayList<>();

        @Override
        protected List<HaniItem> doInBackground(String... arg) {
            try {
                URL url = new URL(arg[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Accept-Charset", "UTF-8");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                connection.connect();

                InputStream input = connection.getInputStream();
                Log.d(TAG, "connection ok....");

                parsing(new InputStreamReader(input, "UTF-8"));
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return list;
        }

        protected void onPostExecute(List<HaniItem> result) {
            MyAdapter adapter = new MyAdapter();
            adapter.setList(result);
            listView.setAdapter(adapter);
        }

        private void parsing(Reader reader) throws Exception {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(reader);
            int eventType = parser.getEventType();
            HaniItem item = null;
            long id = 0;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name = null;
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("item")) {
                            item = new HaniItem();
                            item.id = ++id;
                        } else if (item != null) {
                            if (name.equalsIgnoreCase("title")) {
                                item.title = parser.nextText();
                            } else if (name.equalsIgnoreCase("link")) {
                                item.link = parser.nextText();
                            } else if (name.equalsIgnoreCase("description")) {
                                item.description = parser.nextText();
                            } else if (name.equalsIgnoreCase("pubDate")) {
                                item.pubDate = new Date(parser.nextText());
                            } else if (name.equalsIgnoreCase("subject")) {
                                item.subject = parser.nextText();
                            } else if (name.equalsIgnoreCase("category")) {
                                item.category = parser.nextText();
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("item") && item != null) {
                            list.add(item);
                        }
                        break;
                }
                eventType = parser.next();
            }
        }
    }

    class MyAdapter extends BaseAdapter {
        List<HaniItem> list;

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;

            if (convertView == null) {
                RowBinding binding = RowBinding.inflate(LayoutInflater.from(getContext()), viewGroup, false);
                convertView = binding.getRoot();

                holder = new ViewHolder();
                holder.title = binding.title;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            HaniItem item = list.get(position);

            holder.title.setText(item.title);

            holder.title.setOnClickListener(v -> {
                speakText(item.title); // Speak the article title
                Toast.makeText(getContext(), item.title + " clicked", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.link));
                startActivity(intent);

            });

            convertView.setOnClickListener(v -> {
                // 클릭 시 해당 기사 내용을 보여주는 코드 추가
                showArticleContent(item);
            });

            return convertView;
        }

        class ViewHolder {
            TextView title;
        }

        public void setList(List<HaniItem> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return list.get(i);
        }

        @Override
        public long getItemId(int i) {
            return list.get(i).id;
        }
    }

    // 기사 내용을 보여주는 메서드
    private void showArticleContent(HaniItem item) {
        // 기사 내용을 보여주는 코드 작성
        // 예시로 Toast 메시지로 기사 제목을 보여줍니다.
        Toast.makeText(getContext(), item.title + " 기사 내용 보기", Toast.LENGTH_SHORT).show();
    }

    // TextToSpeech를 사용하여 텍스트를 음성으로 출력
    private void speakText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
}
