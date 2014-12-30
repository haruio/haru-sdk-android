package com.haru.test;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.haru.Entity;
import com.haru.Haru;
import com.haru.HaruException;
import com.haru.ui.PagedEntityAdapter;
import com.haru.callback.DeleteCallback;
import com.haru.callback.SaveCallback;
import com.haru.ui.ViewHolder;

public class EntityActivity extends ActionBarActivity {

    private String CLASS_NAME = "TestEntities";
    private PagedEntityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entity);

        // 리스트 초기화
        ListView listView = (ListView) findViewById(R.id.entity_list);

        // PageAdapter 초기화
        adapter = new PagedEntityAdapter(this, Entity.where(CLASS_NAME)
                , R.layout.elem);
        adapter.setOnViewRenderListener(new PagedEntityAdapter.OnViewRenderListener() {
            @Override
            public void onViewRender(int index, Entity entity, View view) {

                ViewHolder holder = new ViewHolder(view);
                TextView listTitle = holder.findViewById(R.id.listTitle),
                        updatedAt = holder.findViewById(R.id.listUpdatedAt);

                listTitle.setText(entity.getString("content"));

                updatedAt.setText(DateUtils.getRelativeTimeSpanString(
                        entity.getUpdatedAt().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE));
            }
        });
        listView.setAdapter(adapter);

        //
        final EditText contentEdit = (EditText) findViewById(R.id.content_edit);
        final Button contentWrite = (Button) findViewById(R.id.content_write);
        contentWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String content = contentEdit.getText().toString();

                // Save
                final Entity entity = new Entity(CLASS_NAME);
                entity.put("content", content);
                entity.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(HaruException exception) {
                        if (exception != null) {
                            Haru.stackTrace(exception);
                            return;
                        }

                        // Notify to user
                        Toast.makeText(EntityActivity.this,
                                "Saved Entity Id : " + entity.getId(),
                                Toast.LENGTH_SHORT).show();

                        // Update
                        adapter.refreshInBackground();
                    }
                });

                // Clear edittext
                contentEdit.setText("");
            }
        });

        // delete on longpress
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
                EntityActivity.this,
                android.R.layout.simple_list_item_1,
                new String[]{"Make offline", "Edit Entity", "Delete entity"}
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setAdapter(menuAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            entity.saveToLocal();
                        }
                        // 수정
                        else if (i == 1) {
                            final EditText text = new EditText(EntityActivity.this);
                            text.setText(entity.getString("content"));
                            text.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));

                            AlertDialog dialog = new AlertDialog.Builder(EntityActivity.this)
                                    .setTitle("Edit Entity")
                                    .setView(text)
                                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            entity.put("content", text.getText().toString());
                                            entity.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(HaruException error) {
                                                    if (error != null) {
                                                        error.printStackTrace();
                                                        toast(error.getMessage());

                                                    } else {
                                                        adapter.refreshInBackground();
                                                        toast("Saved!");
                                                    }
                                                }
                                            });
                                        }
                                    })
                                    .create();
                            dialog.show();
                        }
                        // 삭제
                        else entity.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(HaruException exception) {
                                if (exception != null) {
                                    exception.printStackTrace();
                                    toast(exception.getMessage());
                                }
                                adapter.refreshInBackground();
                                toast("Deleted.");
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
                            final Entity entity = new Entity(CLASS_NAME);
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
