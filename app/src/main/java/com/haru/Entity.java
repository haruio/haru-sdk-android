package com.haru;

import com.haru.write.Operation;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    private List<Operation> operationQueue;

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

        this.className = className;
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
        }
    }

    protected HashMap<String, Object> getCurrentEntityMap() {
        synchronized (this.lock) {
            HashMap<String, Object> currentData = new HashMap<String, Object>(entityData);
            currentData.putAll(changedData);
            return currentData;
        }
    }

    public void discardChanges() {
        synchronized (this.lock) {
            operationQueue.clear();
            changedData.clear();
            deletedFields.clear();
        }
    }

    public void save() {
        synchronized (this.lock) {

        }
    }

    @Override
    public Object encode() throws Exception {

        return null;
    }
}
