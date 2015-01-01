package com.haru.test;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.haru.HaruException;
import com.haru.helpcenter.FAQ;
import com.haru.helpcenter.HelpCenter;
import com.haru.helpcenter.Notice;
import com.haru.helpcenter.callback.GetCategoryCallback;
import com.haru.helpcenter.callback.GetFAQCallback;
import com.haru.helpcenter.callback.GetNoticeCallback;

import java.util.Iterator;
import java.util.List;

public class HelpCenterFaqActivity extends ActionBarActivity {

    private LinearLayout container;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        // initialize container
        container = (LinearLayout) findViewById(R.id.haru_container);;

        // inflater
        mInflater = LayoutInflater.from(this);

        refresh();
    }

    private void refresh() {

        container.removeAllViews();

        HelpCenter.getFaqCategories(new GetCategoryCallback() {
            @Override
            public void done(List<String> noticeList, HaruException error) {

                Iterator<String> iter = noticeList.iterator();
                while (iter.hasNext()) {
                    String category = iter.next();

                    // Inflate category header view
                    final LinearLayout header = (LinearLayout)
                            mInflater.inflate(R.layout.faq_header_elem, null, false);

                    // Set category text
                    TextView headerText = (TextView) header.findViewById(R.id.headerText);
                    headerText.setText(category);

                    container.addView(header);

                    // Load categories
                    HelpCenter.getFrequentlyAskedQuestions(category, new GetFAQCallback() {
                        @Override
                        public void done(List<FAQ> faqList, HaruException error) {

                            Iterator<FAQ> iter = faqList.iterator();
                            while (iter.hasNext()) {
                                FAQ faq = iter.next();
                                View faqElem = mInflater.inflate(R.layout.faq_elem, null, false);

                                TextView title = (TextView) faqElem.findViewById(R.id.listTitle),
                                        body = (TextView) faqElem.findViewById(R.id.listContent);

                                title.setText(faq.getTitle());
                                body.setText(faq.getBody());

                                header.addView(faqElem);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            refresh();
        }
    }
}
