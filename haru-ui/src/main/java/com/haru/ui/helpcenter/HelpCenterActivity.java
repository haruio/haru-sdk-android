package com.haru.ui.helpcenter;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.haru.helpcenter.FAQ;
import com.haru.ui.R;

/**
 * Help Center Activity
 */
public class HelpCenterActivity extends FragmentActivity
        implements HelpCenterFragment.OnFaqCategorySelectedListener,
                   FaqListFragment.OnFaqSelectedListener {

    private FragmentManager mFragmentManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.haru_activity_fragment);

        setTitle(R.string.haru_help_center);
        setTheme(R.style.Haru_Theme_Light);

        // Show main fragment
        mFragmentManager = getSupportFragmentManager();
        mFragmentManager
                .beginTransaction()
                .add(R.id.haru_container, new HelpCenterFragment())
                .commit();
    }


    @Override
    public void onFAQCategorySelected(String categoryName) {
        // Show FAQ list in category
        mFragmentManager
                .beginTransaction()
                .replace(R.id.haru_container, FaqListFragment.newInstance(categoryName))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onFAQSelected(FAQ faq) {
        // Show FAQ details
        mFragmentManager
                .beginTransaction()
                .replace(R.id.haru_container, FaqDetailFragment.newInstance(faq))
                .addToBackStack(null)
                .commit();
    }
}
