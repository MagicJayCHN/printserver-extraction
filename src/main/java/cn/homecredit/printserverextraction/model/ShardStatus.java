package cn.homecredit.printserverextraction.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShardStatus extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int shardId;
    private Long startId;
    private Long endId;
    private Long currentId;
    private String status;

    // Getters and Setters
}
