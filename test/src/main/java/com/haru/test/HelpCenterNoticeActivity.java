package com.haru.test;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.haru.Haru;
import com.haru.HaruException;
import com.haru.helpcenter.HelpCenter;
import com.haru.helpcenter.Notice;
import com.haru.helpcenter.callback.GetNoticeCallback;

import java.util.ArrayList;

public class HelpCenterNoticeActivity extends ActionBarActivity {

    private NoticeAdapter adapter;
    private ArrayList<Notice> notices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        // 리스트 초기화
        ListView listView = (ListView) findViewById(R.id.notice_list);

        // NoticeAdapter 초기화
        notices = new ArrayList<Notice>();
        adapter = new NoticeAdapter(this, notices);
        listView.setAdapter(adapter);

        refresh();
    }

    private void refresh() {
        HelpCenter.getNoticeList(new GetNoticeCallback() {
            @Override
            public void done(ArrayList<Notice> noticeList, HaruException error) {
                if (error != null) {
                    Haru.stackTrace(error);
                    return;
                }
                notices.clear();
                notices.addAll(noticeList);
                adapter.notifyDataSetChanged();
            }
        });

    }

    public class NoticeAdapter extends BaseAdapter {

        private ArrayList<Notice> notices;
        private LayoutInflater inflater;

        public NoticeAdapter(Context context, ArrayList<Notice> noticeList) {
            this.inflater = LayoutInflater.from(context);
            this.notices = noticeList;
        }

        @Override
        public int getCount() {
            return notices.size();
        }

        @Override
        public Object getItem(int i) {
            return notices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = inflater.inflate(R.layout.notice_elem, null, false);
            }

            Notice notice = notices.get(i);

            TextView title = (TextView) view.findViewById(R.id.listTitle),
                    content = (TextView) view.findViewById(R.id.listContent);

            title.setText(notice.getTitle());
            content.setText(notice.getBody());

            return view;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            refresh();
        }
    }
}
