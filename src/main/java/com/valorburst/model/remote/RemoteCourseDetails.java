package com.valorburst.model.remote;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoteCourseDetails {

    @Id
    @Column(name = "course_details_id")
    private Integer courseDetailsId;

    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "course_details_name")
    private String courseDetailsName;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "create_time")
    private Instant createTime;

    @Column(name = "update_time")
    private Instant updateTime;

    @Column(name = "title_img")
    private String titleImg;

    @Column(name = "content")
    private String content;

    @Column(name = "good_num")
    private Integer goodNum;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "is_price")
    private Integer isPrice;

    @Column(name = "good")
    private Integer good;

    @Column(name = "language_type")
    private String languageType;
}
