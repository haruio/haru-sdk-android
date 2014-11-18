package com.haru;

import android.os.Parcel;
import android.os.Parcelable;

import com.haru.callback.DeleteCallback;
import com.haru.callback.SaveCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;
import com.haru.write.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * 서버에 저장되거나, 로컬에 저장되는 데이터이다.
 * The data that stored into the Haru server or local datastore.
 */
public class Entity implements JsonEncodable, Parcelable {

    final Object lock = new Object();

    private static final String INHERITED = "__inherited";

    protected Map<String, Object> entityData;
    private static boolean isSubclassed = false;

    // 아직 저장되지 않은, 수정된 데이터이다.
    // 엔티티의 원래 데이터이다.
    private Map<String, Object> changedData;

    // 아직 저장되지 않은, 삭제된 필드들이다.
    private List<String> deletedFields;

    // 서버로 보내질 Operation들이다.
    private LinkedList<Operation> operationQueue;
    protected OperationSet operationSet;

    protected String className;
    protected String entityId;
    protected Date createdAt;
    protected Date updatedAt;

    private static HashMap<String, Class<? extends Entity>> subclassedEntityRepository
            = new HashMap<String, Class<? extends Entity>>();

    private boolean isDeleted = false;

    /**
     * Entity를 오버라이딩한 클래스는 서버에 저장될 클래스명을 자신 클래스 이름으로 자동 결정한다.
     */
    protected Entity() {
        this(INHERITED);
    }

    /**
     * Entity를 상속받아 새로운 클래스를 만들었을 경우, 해당 클래스를 등록한다.
     * @param classObject 서브클래싱한 엔티티
     */
    public static void registerSubclass(Class<? extends Entity> classObject) {
        subclassedEntityRepository.put(getClassName(classObject), classObject);
    }

    public static String getClassName(Class<? extends Entity> subclassedClass) {
        String className;

        // find class name
        ClassNameOfEntity classNameAnnotation = subclassedClass.getAnnotation(ClassNameOfEntity.class);
        if (classNameAnnotation == null || classNameAnnotation.value() == null) {
            // use class name as entity className
            className = subclassedClass.getSimpleName();

        } else {
            // from annotation (@ClassNameOfEntity(YET_ANOTHER_NAME))
            className = classNameAnnotation.value();
        }
        return className;
    }

    /**
     * Entity를 상속받고, Entity.registerSubclass로 등록한 클래스에 한하여,
     * ClassName을 통하여 해당 클래스를 찾는다.
     *
     * @param className 클래스 이름 (등록된 이름)
     */
    protected static <T extends Entity> Class<T> findClassByName(String className) {
        return (Class<T>) subclassedEntityRepository.get(className);
    }

    public static <T extends Entity> T create(Class<T> classObject) {
        try {
            return classObject.newInstance();

        } catch (Exception e) {
            if ((e instanceof RuntimeException)) {
                throw ((RuntimeException) e);
            }
            throw new RuntimeException("Failed to create instance of subclass.", e);
        }
    }

    public static <T extends Entity> T create(String className) {
        try {
            return (T) findClassByName(className).newInstance();

        } catch (Exception e) {
            if ((e instanceof RuntimeException)) {
                throw ((RuntimeException) e);
            }
            throw new RuntimeException("Failed to create instance of subclass.", e);
        }
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
        this.operationSet = new OperationSet(className, entityId);

        this.className = className;
//      addOperationToQueue(new CreateEntityOperation(this));
    }

    /**
     * 엔티티를 서버 혹은 로컬에서 가져온다.
     * @param entityId 가져오려는 엔티티의 ID
     * @return 해당 엔티티
     */
    public static final Entity retrieve(String className, String entityId) {
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
            throw new RuntimeException("retrieve : fetch task has interrupted");
        }
    }

    /**
     * Entity를 서버에서 가져온다.
     * 주의: Entity를 상속받은 Subclass에서만 쓰이는 메서드이다!
     *
     * @param entityClass Retrieve할 타입의 서브클래스
     * @param entityId 가져오려는 엔티티의 ID
     * @return 해당 엔티티
     */
    public static <T extends Entity> T retrieve(Class<T> entityClass, String entityId) {
        try {
            final T entity = entityClass.newInstance();
            entity.setEntityId(entityId);

            Task fetchTask = entity.fetchInBackground();
            fetchTask.waitForCompletion();

            if (fetchTask.getError() != null) {
                HaruException exception = (HaruException) fetchTask.getError();
                exception.printStackTrace();
                throw new RuntimeException(exception);
            }
            return (T) fetchTask.getResult();

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("retrieve : fetch task has interrupted.", e);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Class " + entityClass.getName() + " only has a private constructor.");

        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException("Y");

        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new RuntimeException("Class " + entityClass.getName() + " is not inherited Entity class!");
        }
    }

    /**
     * Entity를 서버에서 가져온다.
     * 주의: Entity를 상속받은 Subclass에서만 쓰이는 메서드이다!
     *
     * @param entityClass Retrieve할 타입의 서브클래스
     * @param entityId 가져오려는 엔티티의 ID
     * @return 해당 엔티티
     */
    protected static <T extends Entity> Task<Entity> retrieveTask(Class<T> entityClass,
                                                           String entityId) {
        try {
            final T entity = entityClass.newInstance();
            entity.setEntityId(entityId);

            return entity.fetchInBackground();

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Class " + entityClass.getName() + " only has a private constructor.");

        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException("Y");

        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new RuntimeException("Class " + entityClass.getName() + " is not inherited Entity class!");
        }
    }


    /**
     * Entity를 로컬 데이터스토어로부터 가져온다.
     * @param entityId 가져오려는 엔티티의 ID
     * @return 해당 엔티티
     */
    public static Entity retrieveFromLocal(String className, String entityId) {
        return LocalEntityStore.retriveEntity(className, entityId);
    }


    protected void setEntityId(String entityId) {
        this.entityId = entityId;
        this.operationSet = new OperationSet(className, entityId);
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
     *
     * Entity를 상속받은 (Subclassed) 클래스에서만 호출 가능하다!
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
     * Returns true if it's newly created entity.
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

            // changed only containedIn local
            changedData.put(key, value);
        }
    }

    /**
     * 해당 필드의 데이터를 가져온다.
     * Returns value of the field.
     * @param key 필드 이름
     * @return 해당하는 값, 없을 시 null
     */
    public Object get(String key) {
        synchronized (this.lock) {
            return (changedData.containsKey(key) ? changedData : entityData).get(key);
        }
    }

    public String getString(String key) {
        return String.valueOf(get(key));
    }

    /**
     * 해당 필드의 값을 삭제한다.
     * Remove a value.
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

    public HashMap<String, Object> getAll() {
        HashMap<String, Object> currentData = new HashMap<String, Object>(entityData);
        copyMap(changedData, currentData);
        return currentData;
    }

    /**
     * Entity의 고유 ID를 반환한다.
     * Returns unique id of the Entity.
     * @return Entity Id
     */
    public String getId() {
        return this.entityId;
    }

    /**
     * 이 Entity가 생성된 시간을 반환한다.
     * Returns creation date of the Entity.
     * @return Date
     */
    public Date getCreatedAt() {
        return this.createdAt;
    }

    /**
     * 이 Entity가 마지막으로 수정된 시간을 반환한다.
     * Returns last updated date of the Entity.
     * @return Date
     */
    public Date getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * 이 Entity의 클래스 이름을 반환한다.
     * Returns class name of the Entity.
     * @return Class Name (Collection)
     */
    public String getClassName() {
        return this.className;
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
     * Entity를 로컬 데이터 스토어에 저장한다.
     * Saves entity into the local datastore.
     */
    public void saveToLocal() {
        LocalEntityStore.saveEntity(this, null);
    }

    /**
     * 변경 사항을 백그라운드에서 저장한다.
     */
    public Task saveInBackground() {
        // call with empty callback
        return saveInBackground(new SaveCallback() {
            @Override
            public void done(HaruException exception) { }
        });
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
        Task<HaruResponse> creationTask  = null;
        if(className == "Installations"){
            Haru.logD(" Installations --> %s", className);
            // device token 중복 제거하기 위해서..
            creationTask = new HaruRequest("/installations")
                    .post((JSONObject) this.toJson())
                    .executeAsync();
        } else {
            creationTask = new HaruRequest("/classes/" + className)
                    .post((JSONObject) this.toJson())
                    .executeAsync();
        }

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
                createdAt = parseDate(response.getJsonBody().getString("createdAt"));
                updatedAt = parseDate(response.getJsonBody().getString("updatedAt"));

                entityData.put("createdAt", createdAt.getTime());
                entityData.put("updatedAt", updatedAt.getTime());

                operationSet = new OperationSet(className, entityId);

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

        Task<HaruResponse> task = new HaruRequest("/batch")
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
                entityData.put("updatedAt", updatedAt.getTime());

                if (callback != null) callback.done(null);
                return Entity.this;
            }
        });
    }

    /**
     * Entity의 데이터들을 반환한다.
     * Returns all data of the Entity.
     * @return Data Map (String, Object pair)
     */
    public Map<String, Object> getEntityData() {
        return entityData;
    }

    /**
     * Entity를 삭제한다.
     * Deletes a entity.
     * @return 삭제 태스크
     */
    public Task deleteInBackground() {
        return deleteInBackground(null);
    }

    /**
     * Entity를 삭제한다.
     * Deletes a entity.
     * @param callback 삭제 완료후 호출할 DeleteCallback
     * @return 삭제 태스크
     */
    public Task deleteInBackground(final DeleteCallback callback) {
        synchronized (lock) {
            Task<HaruResponse> deleteTask = new HaruRequest("/classes/" + className + "/" + entityId)
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
     * 서버로부터 Entity 정보를 업데이트한다.
     * 주의: 모든 변경사항은 소실된다.
     */
    public Task<Entity> fetchInBackground() {
        if (isNewEntity()) {
            throw new IllegalStateException("You need to save the object before you fetch it.");
        }
        discardChanges();

        Task<HaruResponse> fetchTask = new HaruRequest("/classes/" + className + "/" + entityId).executeAsync();
        return fetchTask.continueWith(new Continuation<HaruResponse, Entity>() {
            @Override
            public Entity then(Task<HaruResponse> task) throws Exception {
                HaruResponse response = task.getResult();
                if (response.hasError()) {
                    response.getError().printStackTrace();
                    throw response.getError();
                }

                // Fetch some information
                entityData = Haru.convertJsonToMap(response.getJsonBody());
                createdAt = parseDate(response.getJsonBody().getString("createdAt"));
                updatedAt = parseDate(response.getJsonBody().getString("updatedAt"));

                entityData.put("createdAt", createdAt.getTime());
                entityData.put("updatedAt", updatedAt.getTime());

                // Exclude duplicated _id from entityData
                entityData.remove("_id");

                return Entity.this;
            }
        });
    }


    private static Date parseDate(String text) {
        if (text == null) return null;
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
     * @param json JSON Packet
     * @return 변환된 Entity
     */
    static <ENTITY extends Entity> ENTITY fromJson(Class<ENTITY> classObject,
                                                   String className,
                                                   JSONObject json) throws Exception {
        ENTITY entity = classObject.newInstance();
        entity.className = className;
        entity.entityData = Haru.convertJsonToMap(json);
        entity.entityId = json.getString("_id");
        entity.createdAt = parseDate(json.getString("createdAt"));
        entity.updatedAt = parseDate(json.getString("updatedAt"));
        return entity;
    }

    /**
     * JSON을 Entity로 변환한다.
     * @param json JSON Packet
     * @return 변환된 Entity
     */
    static <T extends Entity> T fromJson(Class<T> classObject, JSONObject json) throws Exception {
        T entity = classObject.newInstance();
        entity.entityData = Haru.convertJsonToMap(json);
        entity.entityId = json.getString("_id");
        entity.createdAt = parseDate(json.getString("createdAt"));
        entity.updatedAt = parseDate(json.getString("updatedAt"));
        return entity;
    }


    /**
     * JSON을 Entity로 변환한다.
     * @param json JSON Packet
     * @param entityId 해당 엔티티의 ID
     * @return 변환된 Entity
     */
    static <T extends Entity> T fromJsonToSubclass(Class<T> classObject,
                                                   String className,
                                                   String entityId,
                                                   JSONObject json) throws Exception {
        T entity = classObject.newInstance();
        entity.entityData = Haru.convertJsonToMap(json);
        entity.className = className;
        entity.entityId = entityId;
        entity.operationSet = new OperationSet(className, entityId);
        entity.createdAt = parseDate(json.getString("createdAt"));
        entity.updatedAt = parseDate(json.getString("updatedAt"));
        return entity;
    }

    /**
     * Entity를 JSON 형태로 인코딩한다.
     */
    @Override
    public Object toJson() {
        // TODO: Nested Object에 대한 처리를 하시오.
        HashMap<String, Object> entityMap = getAll();
        return new JSONObject(entityMap);
    }

    protected Entity(Parcel in) {
        changedData = new HashMap();
        deletedFields = new ArrayList();
        operationQueue = new LinkedList<Operation>();

        try {
            entityData = Haru.convertJsonToMap(new JSONObject(in.readString()));
            className = in.readString();
            entityId = in.readString();
            createdAt = new Date(in.readLong());
            updatedAt = new Date(in.readLong());

            operationSet = new OperationSet(className, entityId);

        } catch (JSONException e) {
            throw new RuntimeException("Tried to convert malformed Parcel to entity!");
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Entity를 Parcel 형태로 직렬화한다. 직렬화 시에만 호출된다.
     * @param parcel Parcel
     * @param i
     */
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.toJson().toString());
        parcel.writeString(className);
        parcel.writeString(entityId);
        parcel.writeLong(createdAt.getTime());
        parcel.writeLong(updatedAt.getTime());
    }

    /**
     * Entity를 Parcel 형태로 직렬화한다. 직렬화 시에 사용된다.
     */
    public static final Parcelable.Creator<Entity> CREATOR = new Parcelable.Creator<Entity>() {
        @Override
        public Entity createFromParcel(Parcel in) {
            return new Entity(in);
        }

        @Override
        public Entity[] newArray(int size) {
            return new Entity[size];
        }
    };

}
