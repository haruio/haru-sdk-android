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
import com.haru.helpcenter.HelpCenter;
import com.haru.helpcenter.callback.GetCategoryCallback;
import com.haru.ui.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *  Help Center main screen fragment.
 */
public class HelpCenterFragment extends Fragment {

    private List<String> categories;
    private LinearLayout faqContainer;
    private LayoutInflater mInflater;

    public static interface OnFaqCategorySelectedListener {
        public void onFAQCategorySelected(String categoryName);
    }

    private OnFaqCategorySelectedListener mFaqListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup view, Bundle savedInstanceState) {
        mInflater = inflater;
        View rootView = mInflater.inflate(R.layout.haru_helpcenter, null, false);

        // Send question card
        View sendQuestion = rootView.findViewById(R.id.haru_question_card);
        sendQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SendQuestionDialogBuilder(getActivity()).show();
            }
        });

        // Load category
        categories = new ArrayList<String>();
        faqContainer = (LinearLayout) rootView.findViewById(R.id.haru_faq_card_container);
        loadCategory();

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activity inherits Listener?
        if (activity instanceof OnFaqCategorySelectedListener) {
            mFaqListener = (OnFaqCategorySelectedListener) activity;
        }
    }

    /**
     * Loads FAQ Category List.
     */
    private void loadCategory() {
        HelpCenter.getFaqCategories(new GetCategoryCallback() {
            @Override
            public void done(List<String> categoryList, HaruException error) {
                // error handling
                if (error != null) {
                    Haru.stackTrace(error);
                    return;
                }

                // add categories
                categories.addAll(categoryList);

                // add view
                faqContainer.removeAllViews();
                Iterator<String> iter = categories.iterator();
                while (iter.hasNext()) {
                    View elem = createCategoryView(iter.next());
                    faqContainer.addView(elem);
                }
            }
        });
    }

    /**
     * Make FAQ Category List Row View.
     * @param categoryName FAQ Category Name
     */
    private View createCategoryView(final String categoryName) {
        View view = mInflater.inflate(R.layout.haru_helpcenter_category_elem, null, false);

        TextView title = (TextView) view.findViewById(R.id.haru_faq_category_name),
                count = (TextView) view.findViewById(R.id.haru_faq_category_count);

        title.setText(categoryName);
        count.setText("(0)"); // TODO: Category Count

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFaqListener != null) mFaqListener.onFAQCategorySelected(categoryName);
            }
        });

        return view;
    }
}

