package com.haru.test;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.haru.Entity;
import com.haru.HaruException;
import com.haru.callback.DeleteCallback;
import com.haru.callback.FindCallback;
import com.haru.callback.SaveCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

public class MainActivity extends ActionBarActivity {

    private ArticleAdpater adpater;
    private ArrayList<Entity> articles = new ArrayList<Entity>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 리스트 초기화
        ListView listView = (ListView) findViewById(R.id.listView);
        adpater = new ArticleAdpater(this, articles);
        listView.setAdapter(adpater);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int index, long l) {

                final ArrayAdapter<String> menuAdapter = new ArrayAdapter<String>(
                        MainActivity.this,
                        android.R.layout.simple_list_item_1,
                        new String[] { "삭제" }
                );

                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setAdapter(menuAdapter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (i == 0) {
                                    // 삭제
                                    final Entity entity = articles.get(index);
                                    entity.deleteInBackground(new DeleteCallback() {
                                        @Override
                                        public void done(HaruException exception) {
                                            if (exception != null) {
                                                exception.printStackTrace();
                                                toast(exception.getMessage());
                                            }
                                            articles.remove(index);
                                            adpater.notifyDataSetChanged();
                                            toast("삭제되었습니다.");
                                        }
                                    });
                                }
                            }
                        })
                        .create();
                dialog.show();
                return true;
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {

            Entity.where("TestArticle").findAll(new FindCallback() {
                @Override
                public void done(List<Entity> findResult, HaruException error) {
                    if (error != null) {
                        Log.d("HaruTest", error.getMessage());
                        toast(error.getMessage());
                        return;
                    }
                    articles.clear();
                    articles.addAll(findResult);
                    Log.e("Haru", "MainActivity : Results : " + String.valueOf(findResult.size()));
                    adpater.notifyDataSetChanged();
                }
            });
        }
    }

    private class ArticleAdpater extends BaseAdapter {

        private List<Entity> articles;
        private Context context;
        private LayoutInflater inflater;

        public ArticleAdpater(Context context, List<Entity> articles) {
            this.context = context;
            this.articles = articles;
            inflater = LayoutInflater.from(context);
        }


        @Override
        public int getCount() {
            return articles.size();
        }

        @Override
        public Object getItem(int i) {
            return articles.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = inflater.inflate(R.layout.elem, null, false);
            }
            Entity article = articles.get(i);
            Log.d("Haru", String.valueOf(article.get("title")));
            ((TextView) view.findViewById(R.id.listTitle)).setText((String) article.get("title"));
            return view;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_write) {

            View writeLayout = LayoutInflater.from(this).inflate(R.layout.write_article, null);

            final EditText title = (EditText) writeLayout.findViewById(R.id.title),
                    body = (EditText) writeLayout.findViewById(R.id.body);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("글 쓰기")
                    .setView(writeLayout)
                    .setPositiveButton("작성", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            // 엔티티를 저장한다.
                            final Entity entity = new Entity("TestArticle");
                            entity.put("title", title.getText().toString());
                            entity.put("body", body.getText().toString());
                            entity.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(HaruException error) {
                                    if (error != null) {
                                        Log.d("HaruTest", error.getMessage());
                                        toast(error.getMessage());
                                        return;
                                    }
                                    toast("저장된 메모 : " + entity.getId());

                                    Entity.where("TestArticle").findAll(new FindCallback() {
                                        @Override
                                        public void done(List<Entity> findResult, HaruException error) {
                                            if (error != null) {
                                                Log.d("HaruTest", error.getMessage());
                                                toast(error.getMessage());
                                                return;
                                            }
                                            articles.clear();
                                            articles.addAll(findResult);
                                            adpater.notifyDataSetChanged();
                                        }
                                    });

                                }
                            });
                        }
                    })
                    .create();
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
