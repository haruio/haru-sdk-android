package com.haru;

import com.haru.callback.FindCallback;
import com.haru.callback.GetCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Query {

    /**
     * Entity.where("User").group().greaterThan("10").or().ungroup());
     */

    private Class<? extends Entity> classObject;
    private String className;
    private JSONObject mainQueryObject;
    private JSONObject currentScope;

    private Query(Class<? extends Entity> subclassedClass) {
        this.className = Entity.getClassName(subclassedClass);
        this.classObject = subclassedClass;

        mainQueryObject = new JSONObject();

        // 현재 작업 범위 : 최상위 노드.
        currentScope = mainQueryObject;
    }

    private Query(String className) {
        this.className = className;
        this.classObject = Entity.findClassByName(className);

        if (classObject == null) {
            // 서브클래싱 안된 클래스는 그냥 Entity.
            classObject = Entity.class;
        }

        mainQueryObject = new JSONObject();

        // 현재 작업 범위 : 최상위 노드.
        currentScope = mainQueryObject;
    }

    public static Query where(String className) {
        return new Query(className);
    }

    public Query equalTo(String field, String value) {
        try {
            currentScope.put(field, value);
        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query equalTo(String field, boolean value) {
        try {
            currentScope.put(field, value);
        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query equalTo(String field, double value) {
        try {
            currentScope.put(field, value);
        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query equalTo(String field, int value) {
        try {
            currentScope.put(field, value);
        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query notEqualTo(String field, String value) {
        try {
            JSONObject neClause = new JSONObject();
             neClause.put("$ne", value);
            currentScope.put(field, neClause);
        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query notEqualTo(String field, boolean value) {
        try {
            JSONObject neClause = new JSONObject();
            neClause.put("$ne", value);
            currentScope.put(field, neClause);
        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query notEqualTo(String field, double value) {
        try {
            JSONObject neClause = new JSONObject();
            neClause.put("$ne", value);
            currentScope.put(field, neClause);
        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query notEqualTo(String field, int value) {
        try {
            JSONObject neClause = new JSONObject();
            neClause.put("$ne", value);
            currentScope.put(field, neClause);
        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query greaterThan(String field, int value) {
        try {
            JSONObject gtClause = new JSONObject();
            gtClause.put("$gt", value);
            currentScope.put(field, gtClause);

        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query greaterThan(String field, double value) {
        try {
            JSONObject gtClause = new JSONObject();
            gtClause.put("$gt", value);
            currentScope.put(field, gtClause);

        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query greaterThanOrEqualTo(String field, int value) {
        try {
            JSONObject gteClause = new JSONObject();
            gteClause.put("$gte", value);
            currentScope.put(field, gteClause);

        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query lessThan(String field, int value) {
        try {
            JSONObject ltClause = new JSONObject();
            ltClause.put("$lt", value);
            currentScope.put(field, ltClause);

        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query lessThan(String field, double value) {
        try {
            JSONObject ltClause = new JSONObject();
            ltClause.put("$lt", value);
            currentScope.put(field, ltClause);

        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query lessThanOrEqualTo(String field, int value) {
        try {
            JSONObject lteClause = new JSONObject();
            lteClause.put("$lte", value);
            currentScope.put(field, lteClause);

        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query lessThanOrEqualTo(String field, double value) {
        try {
            JSONObject gteClause = new JSONObject();
            gteClause.put("$lte", value);
            currentScope.put(field, gteClause);

        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query containedIn(String field, List<?> value) {
        try {
            JSONObject inClause = new JSONObject();
            inClause.put("$in", value);
            currentScope.put(field, inClause);

        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query notContainedIn(String field, List<?> value) {
        try {
            JSONObject ninClause = new JSONObject();
            ninClause.put("$nin", value);
            currentScope.put(field, ninClause);

        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query between(String field, int minimum, int maximum) {
        this.greaterThanOrEqualTo(field, minimum);
        this.lessThanOrEqualTo(field, maximum);
        return this;
    }

    public Query group() {
        return this;
    }

    public Query ungroup() {
        return this;
    }

    public void findAll(final FindCallback callback) {
        HaruRequest.Param param = new HaruRequest.Param();
        param.put("where", mainQueryObject.toString());
        Task<HaruResponse> findTask = Haru.newApiRequest("/classes/" + className)
                .get(param)
                .executeAsync();

        findTask.continueWith(new Continuation<HaruResponse, Object>() {
            @Override
            public Object then(Task<HaruResponse> task) throws Exception {

                if (task.isFaulted()) {
                    // Exception
                    task.getError().printStackTrace();
                    callback.done(null, new HaruException(task.getError()));
                    throw task.getError();
                }


                HaruResponse response = task.getResult();
                if (response.hasError()) {
                    // API Error
                    callback.done(null, response.getError());
                    throw response.getError();
                }

                // re-encode results(containedIn JSON format) to Entity Object
                ArrayList<Entity> findResult = new ArrayList<Entity>();
                JSONArray array = response.getJsonBody().getJSONArray("results");
                for (int i=0;i<array.length();i++) {
                    findResult.add(Entity.fromJson(classObject, className, array.getJSONObject(i)));
                }

                callback.done(findResult, null);
                return null;
            }
        });
    }

    public void findOne(final GetCallback callback) {
        HaruRequest.Param param = new HaruRequest.Param();
        param.put("where", mainQueryObject.toString());
        Task<HaruResponse> findTask = Haru.newApiRequest("/classes/" + className)
                .get(param)
                .executeAsync();

        findTask.continueWith(new Continuation<HaruResponse, Object>() {
            @Override
            public Object then(Task<HaruResponse> task) throws Exception {

                if (task.isFaulted()) {
                    // Exception
                    callback.done(null, new HaruException(task.getError()));
                    throw task.getError();
                }

                HaruResponse response = task.getResult();
                if (response.hasError()) {
                    // API Error
                    callback.done(null, response.getError());
                    throw response.getError();
                }

                // re-encode results(containedIn JSON format) to Entity Object
                JSONArray array = response.getJsonBody().getJSONArray("results");

                callback.done(Entity.fromJson(classObject, className, array.getJSONObject(0)), null);
                return null;
            }
        });
    }

    @Override
    public String toString() {
        return mainQueryObject.toString();
    }

}
