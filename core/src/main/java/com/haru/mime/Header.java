package com.haru.mime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Header
  implements Iterable<MinimalField>
{
  private final List<MinimalField> fields;
  private final Map<String, List<MinimalField>> fieldMap;

  public Header()
  {
    this.fields = new LinkedList();
    this.fieldMap = new HashMap();
  }

  public void addField(MinimalField field) {
    if (field == null) {
      return;
    }
    String key = field.getName().toLowerCase(Locale.US);
    List values = (List)this.fieldMap.get(key);
    if (values == null) {
      values = new LinkedList();
      this.fieldMap.put(key, values);
    }
    values.add(field);
    this.fields.add(field);
  }

  public List<MinimalField> getFields() {
    return new ArrayList(this.fields);
  }

  public MinimalField getField(String name) {
    if (name == null) {
      return null;
    }
    String key = name.toLowerCase(Locale.US);
    List list = (List)this.fieldMap.get(key);
    if ((list != null) && (!list.isEmpty())) {
      return (MinimalField)list.get(0);
    }
    return null;
  }

  public List<MinimalField> getFields(String name) {
    if (name == null) {
      return null;
    }
    String key = name.toLowerCase(Locale.US);
    List list = (List)this.fieldMap.get(key);
    if ((list == null) || (list.isEmpty())) {
      return Collections.emptyList();
    }
    return new ArrayList(list);
  }

  public int removeFields(String name)
  {
    if (name == null) {
      return 0;
    }
    String key = name.toLowerCase(Locale.US);
    List removed = (List)this.fieldMap.remove(key);
    if ((removed == null) || (removed.isEmpty())) {
      return 0;
    }
    this.fields.removeAll(removed);
    return removed.size();
  }

  public void setField(MinimalField field) {
    if (field == null) {
      return;
    }
    String key = field.getName().toLowerCase(Locale.US);
    List list = (List)this.fieldMap.get(key);
    if ((list == null) || (list.isEmpty())) {
      addField(field);
      return;
    }
    list.clear();
    list.add(field);
    int firstOccurrence = -1;
    int index = 0;
    for (Iterator it = this.fields.iterator(); it.hasNext(); index++) {
      MinimalField f = (MinimalField)it.next();
      if (f.getName().equalsIgnoreCase(field.getName())) {
        it.remove();
        if (firstOccurrence == -1) {
          firstOccurrence = index;
        }
      }
    }
    this.fields.add(firstOccurrence, field);
  }

  public Iterator<MinimalField> iterator() {
    return Collections.unmodifiableList(this.fields).iterator();
  }

  public String toString()
  {
    return this.fields.toString();
  }
}