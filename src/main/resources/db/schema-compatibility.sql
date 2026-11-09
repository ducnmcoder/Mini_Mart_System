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

IF COL_LENGTH(N'dbo.USER_ACCOUNT', N'deleted_at') IS NULL
    ALTER TABLE dbo.USER_ACCOUNT ADD deleted_at DATETIME2(0) NULL;

IF COL_LENGTH(N'dbo.USER_ACCOUNT', N'account_name') IS NULL
    EXEC(N'ALTER TABLE dbo.USER_ACCOUNT ADD account_name VARCHAR(60) NULL');

IF EXISTS (SELECT 1 FROM dbo.USER_ACCOUNT WHERE account_name IS NULL)
    EXEC(N'UPDATE dbo.USER_ACCOUNT SET account_name = CASE email
        WHEN ''admin@minimart.example'' THEN ''admin''
        WHEN ''owner@minimart.example'' THEN ''owner''
        WHEN ''cashier1@minimart.example'' THEN ''cashier1''
        WHEN ''cashier2@minimart.example'' THEN ''cashier2''
        WHEN ''inventory@minimart.example'' THEN ''inventory''
        ELSE CONCAT(''account_'', account_id) END');

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.USER_ACCOUNT') AND name=N'account_name' AND is_nullable=1)
    EXEC(N'ALTER TABLE dbo.USER_ACCOUNT ALTER COLUMN account_name VARCHAR(60) NOT NULL');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.USER_ACCOUNT') AND name IN (N'UX_USER_ACCOUNT_account_name',N'UQ_USER_ACCOUNT_account_name'))
    EXEC(N'CREATE UNIQUE INDEX UX_USER_ACCOUNT_account_name ON dbo.USER_ACCOUNT(account_name)');
