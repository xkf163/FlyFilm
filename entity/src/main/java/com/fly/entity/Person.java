
package com.fly.entity;

import lombok.Data;
import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by F on 2017/6/27.
 * 原始人物资料
 */
@Entity
@Table(name = "fm_person",uniqueConstraints = {@UniqueConstraint(name = "person_cons",columnNames = {"doubanNo","name"})},indexes = {@Index(name = "person_index",columnList = "name,nameExtend,doubanNo")})
@Data
public class Person extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String nameExtend;

    private String nameCnOther;

    private String nameEnOther;

    //@JsonIgnore
    //@Column(columnDefinition = "TEXT")
    //private String info;

    @Column(columnDefinition = "TEXT")
    private String introduce;

    private String douBanNo;


    private String gender;
    private String birthDay;
    private String birthPlace;

    private String deathDay;

    private String job;
    private String imdbNo;

    //@Lob 通常与@Basic同时使用，提高访问速度
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name=" face_logo", columnDefinition="longblob", nullable=true)
    private byte[] faceLogo;

}
