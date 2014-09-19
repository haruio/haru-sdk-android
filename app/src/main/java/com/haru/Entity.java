package com.haru;

import com.haru.callback.SaveCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;
import com.haru.write.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Haru's read/writable object
 */
public class Entity implements Encodable {

    final Object lock = new Object();

    private static final String TAG = "Haru.Entity";
    private static final String AUTO_CLASS = "__auto";

    // 엔티티의 원래 데이터이다.
    private Map<String, Object> entityData;

    // 아직 저장되지 않은, 수정된 데이터이다.
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

    /**
     * Entity를 오버라이딩한 클래스는 서버에 저장될 클래스명을 자신 클래스 이름으로 자동 결정한다.
     */
    protected Entity() {
        this(AUTO_CLASS);
    }

    public Entity(String className) {
        if (className == null) {
            throw new IllegalArgumentException("You must specify class name to create entity.");
        }

        if (className.equals(AUTO_CLASS)) {
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
    public static Entity findById(String entityId) {
        // TODO: 이 함수를 작성하시오
        return null;
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
            addOperationToQueue(new SetOperation(key, value));

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

    /**
     * 해당 필드를 삭제한다.
     * @param key 삭제할 필드
     */
    public void delete(String key) {
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
        currentData.putAll(changedData);
        return currentData;
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
        try {
            // make request body
            JSONArray operations = (JSONArray) Haru.encode(operationSet);
            JSONObject request = new JSONObject();
            request.put("requests", operations);

            System.out.println(request.toString());

            Task<HaruResponse> task = Haru.newWriteRequest("/batch")
                    .post(request)
                    .executeAsync();

            return task.continueWith(new Continuation<HaruResponse, Entity>() {
                @Override
                public Entity then(Task<HaruResponse> task) throws Exception {
                    HaruResponse response = task.getResult();
                    if (response.getStatusCode() != 200) {
                        callback.done(new HaruException(500, "Internal Server Error"));
                    }

                    // Clear queues and merge changedData
                    changedData.putAll(entityData);
                    discardChanges();

                    // Fetch some information
                    entityId = (String) response.getJsonBody().get("objectId");
                    createdAt = new Date((Long) response.getJsonBody().get("createAt"));
                    updatedAt = new Date((Long) response.getJsonBody().get("updateAt"));

                    callback.done(null);
                    return Entity.this;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void addOperationToQueue(Operation operation) {
        String method = operation.getMethod();

        if (operation instanceof DeleteFieldOperation && operationSet.containsKey("set")) {
            SetOperation setOperations = (SetOperation) operationSet.get("set");
            setOperations.removeOperationByKey(((DeleteFieldOperation) operation).getOriginalValue());

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
     * 서버로부터 정보를 백그라운드에서 업데이트해온다.
     * 모든 변경사항은 소실된다.
     */
    public Task fetchInBackground() {
        if (isNewEntity()) {
            throw new IllegalStateException("You need to save the object before fetch it");
        }
        discardChanges();

        Task<HaruResponse> fetchTask = Haru.newApiRequest("/classes/" + className + "/" + entityId).executeAsync();
        return fetchTask.continueWith(new Continuation<HaruResponse, Entity>() {
            @Override
            public Entity then(Task<HaruResponse> task) throws Exception {
                HaruResponse response = task.getResult();
                if (response.hasApiError()) {
                    throw response.getApiError();
                }

                entityData = Haru.convertJsonToMap(response.getJsonBody());
                return Entity.this;
            }
        });
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
