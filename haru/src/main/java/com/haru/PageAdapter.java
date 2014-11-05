package com.haru;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.haru.callback.FindCallback;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PageAdapter extends BaseAdapter {

    public interface OnViewRenderListener {
        public void onViewRender(int index, Entity entity, View view);
    }

    public static final int DEFAULT_ITEMS_PER_PAGE = 50;

    private Context context;
    private Query query;
    private int itemsPerPage;
    private int layoutResId;
    private int currentPageNo = 0;

    private OnViewRenderListener viewListener;

    private ArrayList<Entity> entities;

    public PageAdapter(Context context, String entityClassName, int layoutResId) {
        this(context, Entity.where(entityClassName), layoutResId, DEFAULT_ITEMS_PER_PAGE);
    }

    public PageAdapter(Context context, String entityClassName, int layoutResId, int itemsPerPage) {
        this(context, Entity.where(entityClassName), layoutResId, itemsPerPage);
    }

    public PageAdapter(Context context, Query query, int layoutResId) {
        this(context, query, layoutResId, DEFAULT_ITEMS_PER_PAGE);

    }

    public PageAdapter(Context context, Query query, int layoutResId, int itemsPerPage) {
        this.context = context;
        this.query = query;
        this.itemsPerPage = itemsPerPage;
        this.layoutResId = layoutResId;

        entities = new ArrayList<Entity>();
        refreshInBackground();
    }

    /**
     * 리스트 내용을 새로고침한다.
     */
    public void refreshInBackground() {
        load(0, true);
    }

    /**
     * 다음 페이지를 로드한다.
     */
    public void loadMore() {
        load(++currentPageNo, false);
    }

    private void load(int pageNo, final boolean shouldClear) {
        query.findAll(new FindCallback() {
            @Override
            public void done(List<Entity> findResult, HaruException error) {
                if (shouldClear) entities.clear();
                entities.addAll(findResult);
                notifyDataSetChanged();
            }
        });
    }

    /**
     * 리스트의 항목을 렌더링할때 호출될 리스너를 설정한다.
     * 일반적인 Adapter의 getView처럼, 이 리스너에서 뷰에 데이터를 채워 넣어서 반환해야 한다.
     *
     * @param listener OnViewRenderListener
     */
    public void setOnViewRenderListener(OnViewRenderListener listener) {
        this.viewListener = listener;
    }

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
    public View getView(int index, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, null);
        }

        if (viewListener != null) {
            viewListener.onViewRender(index, getItem(index), view);
        }

        return view;
    }
}
