package com.valorburst.model.remote;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoteCourse {
    
    @Id
    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "title")
    private String title;

    @Column(name = "title_img")
    private String titleImg;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "classify_id")
    private Integer classifyId;

    @Column(name = "pay_num")
    private Integer payNum;

    @Column(name = "course_label")
    private String courseLabel;

    @Column(name = "img")
    private String img;

    @Column(name = "details")
    private String details;

    @Column(name = "is_delete")
    private Integer isDelete;

    @Column(name = "create_time")
    private Instant createTime;

    @Column(name = "update_time")
    private Instant updateTime;

    @Column(name = "msg_url")
    private String msgUrl;

    @Column(name = "msg_type")
    private Integer msgType;

    @Column(name = "is_recommend")
    private Integer isRecommend;

    @Column(name = "banner_id")
    private Integer bannerId;

    @Column(name = "course_type")
    private Integer courseType;

    @Column(name = "status")
    private Integer status;

    @Column(name = "banner_img")
    private String bannerImg;

    @Column(name = "over")
    private Integer over;

    @Column(name = "is_price")
    private Integer isPrice;

    @Column(name = "view_counts")
    private Integer viewCounts;

    @Column(name = "language_type")
    private String languageType;

    @Column(name = "have_drm")
    private Integer haveDrm;

    @Column(name = "sys_user_ids")
    private String sysUserIds;

    @Column(name = "parent_sys_user_ids")
    private String parentSysUserIds;
}
