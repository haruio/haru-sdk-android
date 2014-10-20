package com.haru;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

public class PageAdapter extends BaseAdapter {

    private ArrayList<Entity> entities;

    @Override
    public int getCount() {
        return entities.size();
    }

    @Override
    public Entity getItem(int index) {
        return entities.get(index);
    }

    @Override
    public long getItemId(int index) {
        return index;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return null;
    }
}
