package com.valorburst.model.local;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_details_price")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDetailsPrice {

    @Id
    private Integer id;

    @Column(name = "price", unique = true)
    private Double price;
}
