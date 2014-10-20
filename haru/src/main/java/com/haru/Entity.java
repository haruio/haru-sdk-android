package com.haru;

import com.haru.callback.DeleteCallback;
import com.haru.callback.GetCallback;
import com.haru.callback.SaveCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;
import com.haru.write.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class Entity implements Encodable {

    final Object lock = new Object();

    private static final String TAG = "Haru.Entity";
    private static final String INHERITED = "__inherited";

    private Map<String, Object> entityData;
    private static boolean isSubclassed = false;

    // 아직 저장되지 않은, 수정된 데이터이다.
    // 엔티티의 원래 데이터이다.
    private Map<String, Object> changedData;

    // 아직 저장되지 않은, 삭제된 필드들이다.
    private List<String> deletedFields;

    // 서버로 보내질 Operation들이다.
    private LinkedList<Operation> operationQueue;
    private OperationSet operationSet;

    private String className;
    private String entityId;
    private Date createdAt;
    private Date updatedAt;

    private boolean isDeleted = false;

    /**
     * Entity를 오버라이딩한 클래스는 서버에 저장될 클래스명을 자신 클래스 이름으로 자동 결정한다.
     */
    protected Entity() {
        this(INHERITED);
    }

    public Entity(String className) {
        if (className == null) {
            throw new IllegalArgumentException("You must specify class name to create entity.");
        }

        if (className.equals(INHERITED)) {
            isSubclassed = true;
            className = this.getClass().getCanonicalName();
        }

        this.entityData = new HashMap();
        this.changedData = new HashMap();
        this.deletedFields = new ArrayList();
        this.operationQueue = new LinkedList<Operation>();
        this.operationSet = new OperationSet();

        this.className = className;
        addOperationToQueue(new CreateEntityOperation(this));
    }

    /**
     * 엔티티를 서버 혹은 로컬에서 가져온다.
     * @param entityId 가져오려는 엔티티의 ID
     * @return 해당 엔티티
     */
    public static Entity findById(String className, String entityId) {
        try {
            final Entity entity = new Entity(className);
            entity.setEntityId(entityId);

            Task fetchTask = entity.fetchInBackground();
            fetchTask.waitForCompletion();

            if (fetchTask.getError() != null) {
                HaruException exception = (HaruException) fetchTask.getError();
                exception.printStackTrace();
            }
            return (Entity) fetchTask.getResult();

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("findById : fetch task has interrupted");
        }
    }

    /**
     * 엔티티를 서버 혹은 로컬에서 가져온다.
     * @param entityId 가져오려는 엔티티의 ID
     * @return 해당 엔티티
     */
    public static Task retrieve(String className, String entityId, GetCallback callback) {
        final Entity entity = new Entity(className);
        entity.setEntityId(entityId);
        return entity.fetchInBackground();
    }

    private void setEntityId(String entityId) {
        this.entityId = entityId;
    }


    /**
     * 조건에 해당하는 엔티티를 검색한다.
     * @param className 검색할 클래스의 이름
     * @return Query
     */
    public static Query where(String className) {
        return Query.where(className);
    }

    /**
     * 조건에 해당하는 엔티티를 검색한다.
     * Entity를 상속받은 (Subclassed) 클래스에서만 호출 가능하다.
     * Entity 클래스에선 Entity.where(String className)
     * @return
     */
    public static Query where() {
        if (!isSubclassed) {
            throw new RuntimeException("You must call Entity.where(String className).");
        }
        return Query.where("Installation");
    }

    /**
     * 새로운 엔티티인지 체크한다.
     */
    public boolean isNewEntity() {
        return entityId == null;
    }

    /**
     * 해당 필드의 데이터를 수정한다.
     * @param key 필드 이름
     * @param value 값
     */
    public void put(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key shouldn't be null");

        } else if (value == null) {
            throw new IllegalArgumentException("value shouldn't be null");
        }

        synchronized (this.lock) {
            if (deletedFields.contains(key)) {
                // undo delete
                deletedFields.remove(key);
            }

            // set operation : add to operation queue
            addOperationToQueue(new UpdateOperation(key, value));

            // changed only in local
            changedData.put(key, value);
        }
    }

    /**
     * 해당 필드의 데이터를 가져온다.
     * @param key 필드 이름
     * @return 해당하는 값, 없을 시 null
     */
    public Object get(String key) {
        synchronized (this.lock) {
            return (changedData.containsKey(key) ? changedData : entityData).get(key);
        }
    }

    public String getString(String key) {
        return (String) get(key);
    }

    /**
     * 해당 필드를 삭제한다.
     * @param key 삭제할 필드
     */
    public void remove(String key) {
        synchronized (this.lock) {
            if (changedData.containsKey(key)) {
                changedData.remove(key);
            }
            deletedFields.add(key);
            addOperationToQueue(new DeleteFieldOperation(key));
        }
    }

    protected HashMap<String, Object> getCurrentEntityMap() {
        HashMap<String, Object> currentData = new HashMap<String, Object>(entityData);
        copyMap(changedData, currentData);
        return currentData;
    }

    /**
     * Entity의 고유 ID를 반환한다.
     * @return Entity Id
     */
    public String getId() {
        return this.entityId;
    }

    /**
     * 이 Entity가 생성된 시간을 반환한다.
     * @return Date
     */
    public Date getCreatedAt() {
        return this.createdAt;
    }

    /**
     * 이 Entity가 마지막으로 수정된 시간을 반환한다.
     * @return Date
     */
    public Date getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * 서버에 저장하지 않은 변경 사항들을 전부 되돌린다.
     */
    public void discardChanges() {
        synchronized (this.lock) {
            operationSet.clear();
            operationQueue.clear();
            changedData.clear();
            deletedFields.clear();
        }
    }

    /**
     * 변경 사항을 백그라운드에서 저장한다.
     */
    public Task saveInBackground() {
        return saveInBackground(null);
    }

    /**
     * 변경 사항을 백그라운드에서 저장한다.
     * @param callback 작업 완료시 실행할 콜백
     */
    public Task saveInBackground(final SaveCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("No callback specified!");
        }

        synchronized (this.lock) {
            if (isNewEntity()) return createEntity(callback);
            return updateEntity(callback);
        }
    }

    /**
     * Entity를 생성한다.
     */
    private Task createEntity(final SaveCallback callback) {
        Task<HaruResponse> creationTask = Haru.newWriteRequest("/classes/" + className)
                .post((JSONObject) this.encode())
                .executeAsync();

        return creationTask.continueWith(new Continuation<HaruResponse, Entity>() {
            @Override
            public Entity then(Task<HaruResponse> task) throws Exception {

                if (task.isFaulted()) {
                    // Exception
                    callback.done(new HaruException("Request error", task.getError()));
                    throw task.getError();
                }

                HaruResponse response = task.getResult();
                if (response.hasError()) {
                    // API Error
                    callback.done(response.getError());
                    return Entity.this;
                }

                // Clear queues and merge changedData
                copyMap(changedData, entityData);
                discardChanges();

                // Fetch some information
                entityId = (String) response.getJsonBody().get("_id");
                createdAt = parseDate(response.getJsonBody().getString("createAt"));
                updatedAt = parseDate(response.getJsonBody().getString("updateAt"));

                callback.done(null);

                return Entity.this;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * Enttiy를 갱신한다.
     */
    private Task updateEntity(final SaveCallback callback) {

        // make request body
        JSONArray operations = (JSONArray) Haru.encode(operationSet);
        JSONObject request = new JSONObject();
        try {
            request.put("requests", operations);

        } catch (Exception e) {
            e.printStackTrace();
        }

        Task<HaruResponse> task = Haru.newWriteRequest("/batch")
                .post(request)
                .executeAsync();

        return task.continueWith(new Continuation<HaruResponse, Entity>() {
            @Override
            public Entity then(Task<HaruResponse> task) throws Exception {
                if (task.isFaulted()) {
                    // Exception
                    callback.done(new HaruException("Request error", task.getError()));
                    throw task.getError();
                }

                HaruResponse response = task.getResult();
                if (response.hasError()) {
                    // API Error
                    callback.done(response.getError());
                    return Entity.this;
                }

                // Clear queues and merge changedData
                copyMap(changedData, entityData);
                discardChanges();

                // Fetch some information
                updatedAt = parseDate(response.getJsonBody().getString("updateAt"));

                if (callback != null) callback.done(null);
                return Entity.this;
            }
        });
    }

    /**
     * Entity를 삭제한다.
     * @return 삭제 태스크
     */
    public Task deleteInBackground() {
        return deleteInBackground(null);
    }

    /**
     * Entity를 삭제한다.
     * @param callback 삭제 완료후 호출할 DeleteCallback
     * @return 삭제 태스크
     */
    public Task deleteInBackground(final DeleteCallback callback) {
        synchronized (lock) {
            Task<HaruResponse> deleteTask = Haru.newWriteRequest("/classes/" + className + "/" + entityId)
                    .delete()
                    .executeAsync();

            deleteTask.continueWith(new Continuation<HaruResponse, Void>() {
                @Override
                public Void then(Task<HaruResponse> task) throws Exception {

                    // cleaning
                    discardChanges();
                    entityId = null;
                    isDeleted = true;

                    if (callback != null) callback.done(task.getResult().getError());
                    return null;
                }
            });

            return deleteTask;
        }
    }

    /**
     * 서버로부터 정보를 백그라운드에서 업데이트해온다.
     * 모든 변경사항은 소실된다.
     */
    public Task fetchInBackground() {
        if (isNewEntity()) {
            throw new IllegalStateException("You need to save the object before you fetch it.");
        }
        discardChanges();

        Task<HaruResponse> fetchTask = Haru.newApiRequest("/classes/" + className + "/" + entityId).executeAsync();
        return fetchTask.continueWith(new Continuation<HaruResponse, Entity>() {
            @Override
            public Entity then(Task<HaruResponse> task) throws Exception {
                HaruResponse response = task.getResult();
                if (response.hasError()) {
                    throw response.getError();
                }

                // Fetch some information
                entityData = Haru.convertJsonToMap(response.getJsonBody());
                createdAt = parseDate(response.getJsonBody().getString("createAt"));
                updatedAt = parseDate(response.getJsonBody().getString("updateAt"));

                // Exclude duplicated createAt, updateAt, _id from entityData
                entityData.remove("createAt");
                entityData.remove("updateAt");
                entityData.remove("_id");

                return Entity.this;
            }
        });
    }


    private static Date parseDate(String text) {
        return new Date(Long.valueOf(text));
    }

    private void copyMap(Map from, Map to) {
        Iterator iter = from.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            to.put(entry.getKey(), entry.getValue());
        }
    }


    private void addOperationToQueue(Operation operation) {
        String method = operation.getMethod();

        if (operation instanceof DeleteFieldOperation && operationSet.containsKey("set")) {
            UpdateOperation updateOperations = (UpdateOperation) operationSet.get("set");
            updateOperations.removeOperationByKey(((DeleteFieldOperation) operation).getOriginalValue());

        }
        if (operationSet.get(method) == null) {
            operationSet.put(operation.getMethod(), operation);

        } else {
            operationSet.get(operation.getMethod()).mergeFromPrevious(operation);
        }

        // add to operation queue
        operationQueue.addLast(operation);
    }

    /**
     * JSON을 Entity로 변환한다.
     * @param json
     * @return
     */
    static Entity fromJSON(String className, JSONObject json) throws Exception {
        Entity entity = new Entity(className);
        entity.entityData = Haru.convertJsonToMap(json);
        entity.entityId = json.getString("_id");
        entity.createdAt = parseDate(json.getString("createAt"));
        entity.updatedAt = parseDate(json.getString("updateAt"));
        return entity;
    }

    /**
     * Entity를 JSON 형태로 인코딩한다.
     */
    @Override
    public Object encode() {
        // TODO: Nested Object에 대한 처리를 하시오.
        HashMap<String, Object> entityMap = getCurrentEntityMap();
        return new JSONObject(entityMap);
    }
}
