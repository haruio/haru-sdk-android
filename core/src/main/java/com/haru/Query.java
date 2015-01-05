package com.haru;

import com.haru.callback.FindCallback;
import com.haru.callback.GetCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 데이터를 검색하기 위한 쿼리 클래스이다.
 * {@link com.haru.Entity#where(String)}을 통해 Chaining 형식으로 생성할 수 있다.
 */
public class Query {

    private Class<? extends Entity> classObject;
    private String className;
    private JSONObject mainQueryObject;
    private JSONObject currentScope;

    private int countPerPage = -1, pageIndex = -1;
    private String sortOption = "";

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
            inClause.put("$in", new JSONArray(value));
            currentScope.put(field, inClause);

        } catch (JSONException e) {
            throw new RuntimeException("Query build error", e);
        }
        return this;
    }

    public Query notContainedIn(String field, List<?> value) {
        try {
            JSONObject ninClause = new JSONObject();
            ninClause.put("$nin", new JSONArray(value));
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

    /**
     * 결과를 한번에 받아오는 것이 아닌, 페이지로 나누어 받아온다 (Pagination).
     *
     * @param resultCountPerPage 페이지당 항목의 갯수
     * @param pageIndex          가져오길 원하는 페이지 인덱스 (ex: pageCount가 10으로 설정시 index 3 = 40~50번대 결과)
     */
    public Query paginate(int resultCountPerPage, int pageIndex) {
        if (resultCountPerPage <= 0) {
            throw new IllegalArgumentException("resultCountPerPage must be greater than 0!");

        } else if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex must be positive number!");
        }

        this.countPerPage = resultCountPerPage;
        this.pageIndex = pageIndex;
        return this;
    }

    /**
     * 주어진 필드를 기준으로 오름차순 정렬한다.
     *
     * @param fieldName 필드
     */
    public Query sortAscending(String fieldName) {
        this.sortOption += fieldName + ",";
        return this;
    }

    /**
     * 주어진 필드를 기준으로 내림차순 정렬한다.
     *
     * @param fieldName 필드
     */
    public Query sortDescending(String fieldName) {
        this.sortOption += "-" + fieldName + ",";
        return this;
    }

    /**
     * 쿼리를 수행한 결과(Entity)의 목록을 받는다.
     *
     * @param callback {@link com.haru.callback.FindCallback}
     */
    public void findAll(final FindCallback callback) {
        Param param = new Param();
        param.put("where", mainQueryObject.toString());
        if (sortOption.length() != 0)
            param.put("sort", sortOption.substring(0, sortOption.length() - 1));

        if (countPerPage != -1 && pageIndex != -1) {
            Param sortParam = new Param();
            sortParam.put("pageSize", countPerPage);
            sortParam.put("pageNumber", pageIndex + 1);
            param.put("page", sortParam);
        }

        Task<HaruResponse> findTask = new HaruRequest("/classes/" + className)
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
                for (int i = 0; i < array.length(); i++) {
                    findResult.add(Entity.fromJson(classObject, className, array.getJSONObject(i)));
                }

                callback.done(findResult, null);
                return null;
            }
        });
    }

    /**
     * 쿼리를 수행하고 1개의 결과만을 받는다.
     *
     * @param callback {@link com.haru.callback.GetCallback}
     */
    public void findOne(final GetCallback callback) {
        Param param = new Param();
        param.put("where", mainQueryObject);
        Task<HaruResponse> findTask = new HaruRequest("/classes/" + className)
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

    public JSONObject toJson() {
        return mainQueryObject;
    }
}
