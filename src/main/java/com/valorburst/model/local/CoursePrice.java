package com.valorburst.model.local;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_price")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePrice {

    @Id
    private Integer id;

    @Column(name = "price", unique = true)
    private Double price;
}
