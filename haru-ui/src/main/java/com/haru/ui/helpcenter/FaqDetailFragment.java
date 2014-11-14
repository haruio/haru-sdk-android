package com.haru.ui.helpcenter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.haru.helpcenter.FAQ;
import com.haru.ui.R;

/**
 *
 */
public class FaqDetailFragment extends Fragment {

    public static Fragment newInstance(FAQ faq) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("faq", faq);

        FaqDetailFragment fragment = new FaqDetailFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup view, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.haru_helpcenter_faq_details, null, false);

        // get faq parameter
        FAQ faq = getArguments().getParcelable("faq");
        if (faq == null) throw new RuntimeException("You must give faq!!");

        // load contents
        TextView title = (TextView) rootView.findViewById(R.id.haru_faq_title),
                body = (TextView) rootView.findViewById(R.id.haru_faq_content);
        title.setText(faq.getTitle());
        body.setText(Html.fromHtml(faq.getBody()));

        return rootView;
    }
}
