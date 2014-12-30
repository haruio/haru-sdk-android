package com.haru;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Entity를 상속받은 새로운 데이터 클래스를 만들 때 (Subclassing),
 * 서버에 저장될 클래스 이름을 수동으로 지정하기 위해서 사용된다. <br><br>
 *
 * 이 어노테이션을 지정하지 않을 시엔 자동으로 해당 클래스의 이름이 사용된다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClassNameOfEntity {
    public abstract String value();
}
