package com.haru.ui;

import android.util.SparseArray;
import android.view.View;

public class ViewHolder {
    private View view;
    private SparseArray<View> viewHolder;

    public ViewHolder(View rootView) {
        this.view = rootView;

        this.viewHolder = (SparseArray<View>) view.getTag();
        if (viewHolder == null) {
            viewHolder = new SparseArray<View>();
            view.setTag(viewHolder);
        }
    }

    public View getView() {
        return view;
    }

    public <T extends View> T findViewById(int id) {
        View childView = viewHolder.get(id);
        if (childView == null) {
            childView = view.findViewById(id);
            viewHolder.put(id, childView);
        }
        return (T) childView;
    }
}
