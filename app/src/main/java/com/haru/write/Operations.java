package com.haru.write;

import java.util.LinkedList;

public class Operations extends LinkedList<Operation> {

    public Operations(Operation operation) {
        addLast(operation);
    }

    public Operations split(Operation operation) {
        int index = this.indexOf(operation);
        if (index == -1) return null;

        this.remove(index);
        return new Operations(operation);
    }
}
