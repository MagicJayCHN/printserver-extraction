1.feature:
    a.可配置，如处理合同类型（status=a,k,s,d,n,r,t）
    b.可启停
    c.可动态扩容
    d.可观测（进度、性能）
    e.可幂等，防止重复处理
    f.支持错误记录、重新处理错误合同

2.todo
    申请prod mysql，创建ddl，导入100w的a状态credit表记录到mysql的表中
    申请prod nas，申请挂载nas
    Apollo prod环境
    上生产需将RestTe mplate restTemplate的ssl关闭

3.其他：
    a.DDl
        CREATE TABLE contract (
        id BIGINT NOT NULL AUTO_INCREMENT,
        status VARCHAR(255),
        contract_no BIGINT,
        contract_status VARCHAR(255),
        cdate DATETIME DEFAULT CURRENT_TIMESTAMP,
        edate DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        creator VARCHAR(255) DEFAULT 'SYSTEM',
        editor VARCHAR(255) DEFAULT 'SYSTEM',
        PRIMARY KEY (id),
        INDEX idx_cdate (cdate),
        INDEX idx_edate (edate),
        INDEX idx_contract_status (contract_status),
        INDEX idx_contract_no (contract_no),
        INDEX idx_status (status)
        );

        CREATE TABLE failed_contract (
        id BIGINT NOT NULL AUTO_INCREMENT,
        original_contract_id BIGINT,
        reason VARCHAR(255),
        doc_types TEXT,
        cdate DATETIME DEFAULT CURRENT_TIMESTAMP,
        edate DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        creator VARCHAR(255) DEFAULT 'SYSTEM',
        editor VARCHAR(255) DEFAULT 'SYSTEM',
        PRIMARY KEY (id),
        INDEX idx_cdate (cdate),
        INDEX idx_edate (edate),
        FOREIGN KEY (original_contract_id) REFERENCES contract(id)
        );
        
        
        
        CREATE TABLE report (
        id BIGINT NOT NULL AUTO_INCREMENT,
        report_size BIGINT,
        report_path VARCHAR(255),
        report_type VARCHAR(255),
        contract_id BIGINT,
        contract_no BIGINT,
        cdate DATETIME DEFAULT CURRENT_TIMESTAMP,
        edate DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        creator VARCHAR(255) DEFAULT 'SYSTEM',
        editor VARCHAR(255) DEFAULT 'SYSTEM',
        PRIMARY KEY (id),
        INDEX idx_cdate (cdate),
        INDEX idx_edate (edate),
        INDEX idx_contract_no (contract_no),
        FOREIGN KEY (contract_id) REFERENCES contract(id)
        );
        
        CREATE TABLE shard_status (
        shard_id INT NOT NULL AUTO_INCREMENT,
        start_id BIGINT,
        end_id BIGINT,
        current_id BIGINT,
        status VARCHAR(255),
        cdate DATETIME DEFAULT CURRENT_TIMESTAMP,
        edate DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        creator VARCHAR(255) DEFAULT 'SYSTEM',
        editor VARCHAR(255) DEFAULT 'SYSTEM',
        PRIMARY KEY (shard_id),
        INDEX idx_cdate (cdate),
        INDEX idx_edate (edate)，
        INDEX idx_status (status)
        );
        
    b.从Oracle准备contract数据,插入contract表
        select c.EVID_SRV CONTRACT_NO, status CONTRACT_STATUS, 'PENDING' STATUS from CREDIT c where c.STATUS='a' and rownum<1000

    c.还原表数据，供start、progress、stop测试
        UPDATE contract SET status='PENDING' WHERE 1=1;
        UPDATE contract SET edate=cdate WHERE 1=1;
        UPDATE shard_status SET status='PENDING' WHERE 1=1;
        UPDATE shard_status SET current_id=NULL WHERE 1=1;
        UPDATE shard_status SET editor='SYSTEM' WHERE 1=1;
        truncate failed_contract;
        truncate report;


4.接口测试
    ###任务分片
    POST http://localhost:8080/api/task/initializeShards
    Content-Type: application/json
    
    {
    "shardSize": 100
    }
    
    ###启动一个节点
    POST http://localhost:8080/api/task/start
    Content-Type: application/json
    
    {
    "currentPod": 0,
    "allPods": 1
    }
    
    ###关闭一个节点
    POST http://localhost:8080/api/task/stop
    Content-Type: application/json
    
    {
    "currentPod": 0,
    "allPods": 1
    }
    
    ###查询进度
    GET http://localhost:8080/api/statistics/task-progress


5.uat测试结果(5pod)
{
"totalContract": 99999.0,
"finishContractPercentage": 0.893,
"contractProcessVelocityPerSecond": 2.967,
"estimatedRemainingSeconcd": 33405.27,
"estimatedRemainingHour": 9.279,
"finishShardPercentage": 0.0,
"hdssApiQpsPerNode": 14.617
}

{
"totalContract": 99999.0,
"finishContractPercentage": 0.951,
"contractProcessVelocityPerSecond": 2.972,
"estimatedRemainingSeconcd": 33328.454,
"estimatedRemainingHour": 9.258,
"finishShardPercentage": 0.0,
"hdssApiQpsPerNode": 14.383
}
{
"totalContract": 99999.0,
"finishContractPercentage": 0.995,
"contractProcessVelocityPerSecond": 2.988,
"estimatedRemainingSeconcd": 33134.002,
"estimatedRemainingHour": 9.204,
"finishShardPercentage": 0.0,
"hdssApiQpsPerNode": 14.4
}
{
"totalContract": 99999.0,
"finishContractPercentage": 1.033,
"contractProcessVelocityPerSecond": 3.003,
"estimatedRemainingSeconcd": 32956.732,
"estimatedRemainingHour": 9.155,
"finishShardPercentage": 0.0,
"hdssApiQpsPerNode": 14.767
}
{
"totalContract": 99999.0,
"finishContractPercentage": 1.575,
"contractProcessVelocityPerSecond": 3.094,
"estimatedRemainingSeconcd": 31808.137,
"estimatedRemainingHour": 8.836,
"finishShardPercentage": 0.0,
"hdssApiQpsPerNode": 14.483
}
{
"totalContract": 99999.0,
"finishContractPercentage": 1.741,
"contractProcessVelocityPerSecond": 3.12,
"estimatedRemainingSeconcd": 31492.225,
"estimatedRemainingHour": 8.748,
"finishShardPercentage": 0.0,
"hdssApiQpsPerNode": 14.483
}
