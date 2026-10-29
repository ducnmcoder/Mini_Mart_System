/*
 * Compatibility migration for databases created from an older MiniMart script.
 * Spring executes this before Hibernate validates the mapped entities.
 */
IF OBJECT_ID(N'dbo.[AUDIT_LOG]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[AUDIT_LOG] (
        audit_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_AUDIT_LOG PRIMARY KEY,
        actor_id BIGINT NULL,
        action VARCHAR(80) NOT NULL,
        entity_name VARCHAR(80) NOT NULL,
        entity_id VARCHAR(80) NULL,
        old_values NVARCHAR(MAX) NULL,
        new_values NVARCHAR(MAX) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_AUDIT_LOG_created DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_AUDIT_LOG_USER_ACCOUNT FOREIGN KEY (actor_id)
            REFERENCES dbo.[USER_ACCOUNT](account_id),
        CONSTRAINT CK_AUDIT_LOG_old_json CHECK (old_values IS NULL OR ISJSON(old_values)=1),
        CONSTRAINT CK_AUDIT_LOG_new_json CHECK (new_values IS NULL OR ISJSON(new_values)=1)
    )
    CREATE INDEX IX_AUDIT_LOG_actor_date
        ON dbo.[AUDIT_LOG](actor_id, created_at DESC)
END;
