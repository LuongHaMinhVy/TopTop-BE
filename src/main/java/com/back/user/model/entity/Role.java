package com.back.user.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "roles")
public class Role extends BaseEntity{

    @Enumerated(EnumType.STRING)
    private RoleName name;

    @Column(length = 255)
    private String description;
}