package com.haru;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.haru.task.Task;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 *  Entity를 로컬에 저장한다.
 */
class LocalEntityStore {

    private static final String DB_NAME = "local_entities";
    private static final int DB_VERSION = 1;

    private static SQLiteOpenHelper sqLiteHelper;

    static void initialize(Context context) {
        sqLiteHelper =  new SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {
                createDatabase(sqLiteDatabase);
            }

            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
                // 기존 (legacy) 데이터를 로드한다.

                // 기존 테이블을 전부 날린다.
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS HaruEntity");

                // 새로운 테이블을 생성한다.
                this.onCreate(sqLiteDatabase);

                // 다시 데이터를 넣는다.
            }
        };

        sqLiteHelper.getWritableDatabase();
    }

    /**
     * 데이터베이스 스키마 구조를 생성한다.
     * @param db
     */
    private static void createDatabase(SQLiteDatabase db) {

        Log.i("Haru", "Creating DB");
        String sql = "CREATE TABLE HaruEntity ("
                + "id TEXT PRIMARY KEY,"
                + "className TEXT NOT NULL,"
                + "entityId TEXT NOT NULL,"
                + "data TEXT,"
                + "tag TEXT,"
                + "UNIQUE(className, entityId));";

        db.execSQL(sql);
    }

    /**
     * Entity를 로컬 데이터스토어에 저장한다.
     * @param entity 저장할 엔티티
     * @param tag 엔티티를 저장할 때 로컬 데이터스토어에 붙일 태그.
     *              null이 주어질 시 태그를 붙이지 않는다.
     */
    static void saveEntity(Entity entity, String tag) {

        SQLiteDatabase sqLiteDatabase = sqLiteHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("className", entity.getClassName());
        values.put("entityId", entity.getId());
        values.put("data", entity.toJson().toString());
        if (tag != null) values.put("tag", tag);

        sqLiteDatabase.beginTransaction();
        sqLiteDatabase.insertWithOnConflict("HaruEntity", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
        sqLiteDatabase.close();
    }

    private static <T extends Entity> ArrayList<T> retrieve(String className,
                                              String sql,
                                              String[] selectionArgs) {
        SQLiteDatabase sqLiteDatabase = sqLiteHelper.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(sql, selectionArgs);

        if (cursor == null) {
            throw new RuntimeException("Failed to query to local datastore.");
        }

        // Convert JSON Data to Entity object
        try {
            ArrayList<T> entities = new ArrayList<T>();
            //cursor.moveToFirst();

            Log.i("Haru", "Database : " + className);

            while (cursor.moveToNext()) {
                Log.i("Haru", " => element!");
                entities.add((T) Entity.fromJsonToSubclass(Entity.findClassByName(className),
                        cursor.getString(cursor.getColumnIndex("entityId")),
                        new JSONObject(cursor.getString(cursor.getColumnIndex("data")))));
            }
            Log.i("Haru", "Done <=");
            return entities;

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert entity from local data: Malformed JSON", e);
        }
    }

    /**
     * Entity를 로컬 데이터스토어로부터 불러온다.
     * @param className Entity의 className (Collection name)
     * @param entityId Entity의 ID
     */
    static <T extends Entity> T retriveEntity(String className, String entityId) {

        return (T) retrieve(className,
                "SELECT data, entityId FROM HaruEntity WHERE className=? AND entityId=?",
                new String[] { className, entityId }).get(0);
    }

    /**
     * 한 Class 안의 모든 Entity를 로컬 데이터스토어로부터 불러온다.
     * @param className Entity의 className (Collection name)
     * @return Entity List
     */
    static <T extends Entity> ArrayList<T> retrieveAllEntities(String className) {
        return retrieve(className,
                "SELECT data, entityId FROM HaruEntity WHERE className=?",
                new String[] { className });
    }

    /**
     * 한 Class 안에서 특정 태그를 가진 모든 Entity를 로컬 데이터스토어로부터 불러온다.
     * @param className Entity의 className (Collection name)
     * @param tag 태그 이름
     * @return Entity List
     */
    static <T extends Entity> ArrayList<T> retrieveEntitiesByTag(String className, String tag) {
        return retrieve(className,
                "SELECT data, entityId FROM HaruEntity WHERE className=? AND tag=?",
                new String[] { className, tag });
    }

    static Task saveEntityToLocalInBackground(final Entity entity) {
        return Task.call(new Callable<Entity>() {
            @Override
            public Entity call() throws Exception {
                saveEntity(entity, null);
                return entity;
            }
        });
    }

    /**
     * 해당 Entity를 로컬에서 삭제한다.
     * @param entity 삭제하길 원하는 Entity
     */
    static void deleteEntityFromLocal(Entity entity) {
        SQLiteDatabase sqLiteDatabase = sqLiteHelper.getWritableDatabase();
        sqLiteDatabase.beginTransaction();
        sqLiteDatabase.delete("HaruEntity", "entityId=?", new String[] { entity.getId() });
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
        sqLiteDatabase.close();
    }

    /**
     * 해당 태그에 해당하는 Entity를 전부 삭제한다.
     * @param className 클래스 이름 (Collection name)
     * @param tag 태그 이름
     */
    static void deleteTagFromLocal(String className, String tag) {
        SQLiteDatabase sqLiteDatabase = sqLiteHelper.getWritableDatabase();
        sqLiteDatabase.beginTransaction();
        sqLiteDatabase.delete("HaruEntity", "className=? AND tag=?",
                new String[] { className, tag });
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
        sqLiteDatabase.close();
    }
}
