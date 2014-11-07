package com.haru.examplememo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.haru.Entity;
import com.haru.HaruException;
import com.haru.ui.PageAdapter;
import com.haru.callback.DeleteCallback;
import com.haru.callback.SaveCallback;
import com.haru.ui.ViewHolder;

public class MainActivity extends ActionBarActivity {

    private PageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 리스트 초기화
        ListView listView = (ListView) findViewById(R.id.listView);

        // PageAdapter 초기화
        adapter = new PageAdapter(this, "TestArticle", R.layout.elem);
        adapter.setOnViewRenderListener(new PageAdapter.OnViewRenderListener() {
            @Override
            public void onViewRender(int index, Entity article, View view) {

                ViewHolder holder = new ViewHolder(view);
                TextView listTitle = holder.findViewById(R.id.listTitle),
                        updatedAt = holder.findViewById(R.id.listUpdatedAt);

                listTitle.setText(article.getString("title"));

                updatedAt.setText(DateUtils.getRelativeTimeSpanString(
                        article.getUpdatedAt().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE));
            }
        });
        listView.setAdapter(adapter);


/*
        User user = new User();
        user.setUserName("retail3210");
        user.setEmail("retail3210@gmail.com");
        user.setPassword("1234");
        user.signInInBackground(new LoginCallback() {
            @Override
            public void done(User user, HaruException error) {
                if (error != null) {
                    Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                    return;
                }

                Toast.makeText(MainActivity.this, user.getId(), Toast.LENGTH_SHORT).show();
            }
        });*/

        // 길게 눌렀을 시 삭제
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int index, long l) {
                deleteItem(adapter.getItem(index));
                return true;
            }
        });
    }

    /**
     * 리스트의 해당 아이템을 삭제한다.
     * @param entity 엔티티
     */
    private void deleteItem(final Entity entity) {
        final ArrayAdapter<String> menuAdapter = new ArrayAdapter<String>(
                MainActivity.this,
                android.R.layout.simple_list_item_1,
                new String[]{"오프라인", "취소"}
        );

        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setAdapter(menuAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            entity.saveToLocal();
                        }
                        // 삭제
                        else entity.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(HaruException exception) {
                                if (exception != null) {
                                    exception.printStackTrace();
                                    toast(exception.getMessage());
                                }
                                adapter.notifyDataSetChanged();
                                toast("삭제되었습니다.");
                            }
                        });
                    }
                })
                .create();
        dialog.show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            adapter.refreshInBackground();
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

                                    adapter.refreshInBackground();
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
