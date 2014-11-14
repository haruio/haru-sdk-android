package com.haru.ui.helpcenter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.haru.Haru;
import com.haru.HaruException;
import com.haru.helpcenter.FAQ;
import com.haru.helpcenter.HelpCenter;
import com.haru.helpcenter.callback.GetFAQCallback;
import com.haru.ui.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class FaqListFragment extends Fragment {

    private String categoryName;
    private List<FAQ> faqs;
    private LinearLayout faqContainer;
    private LayoutInflater mInflater;

    public static interface OnFaqSelectedListener {
        public void onFAQSelected(FAQ faq);
    }
    private OnFaqSelectedListener mFaqListener;

    public static Fragment newInstance(String categoryName) {
        Bundle bundle = new Bundle();
        bundle.putString("categoryName", categoryName);

        FaqListFragment fragment = new FaqListFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup view, Bundle savedInstanceState) {
        mInflater = inflater;
        View rootView = mInflater.inflate(R.layout.haru_helpcenter_faq, null, false);

        // category Name
        categoryName = getArguments().getString("categoryName");
        if (categoryName == null) throw new RuntimeException("You must give category name to load FAQ");

        TextView title = (TextView) rootView.findViewById(R.id.haru_faq_category_name);
        title.setText(categoryName);

        // Load FAQs
        faqs = new ArrayList<FAQ>();
        faqContainer = (LinearLayout) rootView.findViewById(R.id.haru_faq_card_container);
        loadFaq();

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // activity inherits OnFAQSelectedListener?
        if (activity instanceof OnFaqSelectedListener) {
            mFaqListener = (OnFaqSelectedListener) activity;
        }
    }

    /**
     * Loads FAQs in this category.
     */
    private void loadFaq() {
        HelpCenter.getFrequentlyAskedQuestions(categoryName, new GetFAQCallback() {
            @Override
            public void done(List<FAQ> faqList, HaruException error) {
                // error handling
                if (error != null) {
                    Haru.stackTrace(error);
                    return;
                }

                // add categories
                faqs.addAll(faqList);

                // add views
                faqContainer.removeAllViews();
                Iterator iter = faqs.iterator();
                while (iter.hasNext()) {
                    View elem = createCategoryView((FAQ) iter.next());
                    faqContainer.addView(elem);
                }
            }
        });
    }

    /**
     * Make FAQ list row view.
     * @param faq FAQ Object
     */
    private View createCategoryView(final FAQ faq) {
        View view = mInflater.inflate(R.layout.haru_helpcenter_faq_elem, null, false);

        TextView title = (TextView) view.findViewById(R.id.haru_faq_title);
        title.setText(faq.getTitle());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFaqListener != null) mFaqListener.onFAQSelected(faq);
            }
        });

        return view;
    }
}
