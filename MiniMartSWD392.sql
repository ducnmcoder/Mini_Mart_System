/*
 Mini Mart — SQL Server 2022 / SSMS
 Database schema based on the 23 entities and 40 relationships in the one-page ERD.

 Run the WHOLE file in SSMS (Execute/F5). GO is a batch separator understood by SSMS.
 The script creates MiniMartSWD392 if absent; it never drops or truncates existing data.
 It is safe to rerun against the database created by this file: table and demo-row
 creation is guarded. An existing, incompatible schema is NOT silently migrated.

 Security: demo accounts are PENDING_PASSWORD. Their password_hash values are
 deliberately invalid placeholders. Set a real BCrypt hash in the application
 before activating any account; never use a plain-text/common demo password.

 Inventory rule: services must atomically update INVENTORY and insert the matching
 STOCK_TRANSACTION when a document is confirmed. The sample rows below represent
 the final balance after the demonstrated receipt/issue/sale/refund/adjustment.
*/

IF DB_ID(N'MiniMartSWD392') IS NULL
    EXEC(N'CREATE DATABASE [MiniMartSWD392]');
GO
USE [MiniMartSWD392];                        
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

/* 1. Identity and governance */
IF OBJECT_ID(N'dbo.[ROLE]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[ROLE] (
        role_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ROLE PRIMARY KEY,
        role_name VARCHAR(30) NOT NULL CONSTRAINT UQ_ROLE_name UNIQUE,
        description NVARCHAR(255) NULL,
        is_active BIT NOT NULL CONSTRAINT DF_ROLE_active DEFAULT (1),
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_ROLE_created DEFAULT SYSUTCDATETIME(),
        CONSTRAINT CK_ROLE_name CHECK (role_name IN ('ADMIN','OWNER','SALE_STAFF','INVENTORY_STAFF'))
    );
END;
GO

IF OBJECT_ID(N'dbo.[USER_ACCOUNT]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[USER_ACCOUNT] (
        account_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_USER_ACCOUNT PRIMARY KEY,
        role_id BIGINT NOT NULL,
        email VARCHAR(255) NOT NULL CONSTRAINT UQ_USER_ACCOUNT_email UNIQUE,
        account_name VARCHAR(60) NOT NULL CONSTRAINT UQ_USER_ACCOUNT_account_name UNIQUE,
        password_hash VARCHAR(255) NOT NULL,
        full_name NVARCHAR(120) NOT NULL,
        phone VARCHAR(20) NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_USER_ACCOUNT_status DEFAULT ('PENDING_PASSWORD'),
        last_login_at DATETIME2(0) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_USER_ACCOUNT_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_USER_ACCOUNT_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_USER_ACCOUNT_ROLE FOREIGN KEY (role_id) REFERENCES dbo.[ROLE](role_id),
        CONSTRAINT CK_USER_ACCOUNT_status CHECK (status IN ('PENDING_PASSWORD','ACTIVE','LOCKED','INACTIVE'))
    );
    CREATE UNIQUE INDEX UX_USER_ACCOUNT_phone ON dbo.[USER_ACCOUNT](phone) WHERE phone IS NOT NULL;
END;
GO

IF COL_LENGTH(N'dbo.USER_ACCOUNT', N'account_name') IS NULL
BEGIN
    ALTER TABLE dbo.[USER_ACCOUNT] ADD account_name VARCHAR(60) NULL;
    EXEC(N'UPDATE dbo.[USER_ACCOUNT] SET account_name = CASE email
        WHEN ''admin@minimart.example'' THEN ''admin''
        WHEN ''owner@minimart.example'' THEN ''owner''
        WHEN ''cashier1@minimart.example'' THEN ''cashier1''
        WHEN ''cashier2@minimart.example'' THEN ''cashier2''
        WHEN ''inventory@minimart.example'' THEN ''inventory''
        ELSE CONCAT(''account_'', account_id) END');
    ALTER TABLE dbo.[USER_ACCOUNT] ALTER COLUMN account_name VARCHAR(60) NOT NULL;
    CREATE UNIQUE INDEX UX_USER_ACCOUNT_account_name ON dbo.[USER_ACCOUNT](account_name);
END;
GO

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
        CONSTRAINT FK_AUDIT_LOG_USER_ACCOUNT FOREIGN KEY (actor_id) REFERENCES dbo.[USER_ACCOUNT](account_id),
        CONSTRAINT CK_AUDIT_LOG_old_json CHECK (old_values IS NULL OR ISJSON(old_values)=1),
        CONSTRAINT CK_AUDIT_LOG_new_json CHECK (new_values IS NULL OR ISJSON(new_values)=1)
    );
    CREATE INDEX IX_AUDIT_LOG_actor_date ON dbo.[AUDIT_LOG](actor_id, created_at DESC);
END;
GO

/* 2. Product catalog and locations */
IF OBJECT_ID(N'dbo.[CATEGORY]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[CATEGORY] (
        category_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CATEGORY PRIMARY KEY,
        parent_category_id BIGINT NULL,
        category_name NVARCHAR(120) NOT NULL CONSTRAINT UQ_CATEGORY_name UNIQUE,
        description NVARCHAR(500) NULL,
        is_active BIT NOT NULL CONSTRAINT DF_CATEGORY_active DEFAULT (1),
        CONSTRAINT FK_CATEGORY_parent FOREIGN KEY (parent_category_id) REFERENCES dbo.[CATEGORY](category_id),
        CONSTRAINT CK_CATEGORY_not_self CHECK (parent_category_id IS NULL OR parent_category_id <> category_id)
    );
END;
GO

IF OBJECT_ID(N'dbo.[SUPPLIER]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[SUPPLIER] (
        supplier_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SUPPLIER PRIMARY KEY,
        supplier_code VARCHAR(30) NOT NULL CONSTRAINT UQ_SUPPLIER_code UNIQUE,
        supplier_name NVARCHAR(160) NOT NULL,
        email VARCHAR(255) NULL,
        phone VARCHAR(20) NULL,
        address NVARCHAR(500) NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_SUPPLIER_status DEFAULT ('ACTIVE'),
        CONSTRAINT CK_SUPPLIER_status CHECK (status IN ('ACTIVE','INACTIVE'))
    );
END;
GO

IF OBJECT_ID(N'dbo.[PRODUCT]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[PRODUCT] (
        product_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PRODUCT PRIMARY KEY,
        category_id BIGINT NOT NULL,
        supplier_id BIGINT NULL,
        sku VARCHAR(50) NOT NULL CONSTRAINT UQ_PRODUCT_sku UNIQUE,
        barcode VARCHAR(80) NULL,
        product_name NVARCHAR(180) NOT NULL,
        cost_price DECIMAL(18,2) NOT NULL,
        selling_price DECIMAL(18,2) NOT NULL,
        unit VARCHAR(30) NOT NULL,
        reorder_level INT NOT NULL CONSTRAINT DF_PRODUCT_reorder DEFAULT (0),
        status VARCHAR(20) NOT NULL CONSTRAINT DF_PRODUCT_status DEFAULT ('ACTIVE'),
        CONSTRAINT FK_PRODUCT_CATEGORY FOREIGN KEY (category_id) REFERENCES dbo.[CATEGORY](category_id),
        CONSTRAINT FK_PRODUCT_SUPPLIER FOREIGN KEY (supplier_id) REFERENCES dbo.[SUPPLIER](supplier_id),
        CONSTRAINT CK_PRODUCT_prices CHECK (cost_price >= 0 AND selling_price >= 0),
        CONSTRAINT CK_PRODUCT_reorder CHECK (reorder_level >= 0),
        CONSTRAINT CK_PRODUCT_status CHECK (status IN ('ACTIVE','INACTIVE','DISCONTINUED'))
    );
    CREATE UNIQUE INDEX UX_PRODUCT_barcode ON dbo.[PRODUCT](barcode) WHERE barcode IS NOT NULL;
    CREATE INDEX IX_PRODUCT_category ON dbo.[PRODUCT](category_id);
    CREATE INDEX IX_PRODUCT_supplier ON dbo.[PRODUCT](supplier_id);
END;
GO

IF OBJECT_ID(N'dbo.[STORAGE_LOCATION]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[STORAGE_LOCATION] (
        location_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_STORAGE_LOCATION PRIMARY KEY,
        location_code VARCHAR(30) NOT NULL CONSTRAINT UQ_STORAGE_LOCATION_code UNIQUE,
        location_name NVARCHAR(120) NOT NULL,
        description NVARCHAR(255) NULL,
        is_active BIT NOT NULL CONSTRAINT DF_STORAGE_LOCATION_active DEFAULT (1)
    );
END;
GO

IF OBJECT_ID(N'dbo.[INVENTORY]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[INVENTORY] (
        inventory_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_INVENTORY PRIMARY KEY,
        product_id BIGINT NOT NULL,
        location_id BIGINT NOT NULL,
        quantity_on_hand INT NOT NULL CONSTRAINT DF_INVENTORY_on_hand DEFAULT (0),
        quantity_reserved INT NOT NULL CONSTRAINT DF_INVENTORY_reserved DEFAULT (0),
        last_counted_at DATETIME2(0) NULL,
        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_INVENTORY_updated DEFAULT SYSUTCDATETIME(),
        row_version ROWVERSION,
        CONSTRAINT UQ_INVENTORY_product_location UNIQUE (product_id, location_id),
        CONSTRAINT FK_INVENTORY_PRODUCT FOREIGN KEY (product_id) REFERENCES dbo.[PRODUCT](product_id),
        CONSTRAINT FK_INVENTORY_LOCATION FOREIGN KEY (location_id) REFERENCES dbo.[STORAGE_LOCATION](location_id),
        CONSTRAINT CK_INVENTORY_quantities CHECK (quantity_on_hand >= 0 AND quantity_reserved >= 0 AND quantity_reserved <= quantity_on_hand)
    );
    CREATE INDEX IX_INVENTORY_location ON dbo.[INVENTORY](location_id);
END;
GO

/* 3. Inventory documents: each header has one or more items when CONFIRMED.
   This cross-row invariant is enforced by the application transaction. */
IF OBJECT_ID(N'dbo.[GOODS_RECEIPT]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[GOODS_RECEIPT] (
        receipt_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_GOODS_RECEIPT PRIMARY KEY,
        receipt_no VARCHAR(40) NOT NULL CONSTRAINT UQ_GOODS_RECEIPT_no UNIQUE,
        supplier_id BIGINT NULL,
        received_by BIGINT NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_GOODS_RECEIPT_status DEFAULT ('DRAFT'),
        received_at DATETIME2(0) NOT NULL CONSTRAINT DF_GOODS_RECEIPT_received DEFAULT SYSUTCDATETIME(),
        note NVARCHAR(500) NULL,
        CONSTRAINT FK_GOODS_RECEIPT_SUPPLIER FOREIGN KEY (supplier_id) REFERENCES dbo.[SUPPLIER](supplier_id),
        CONSTRAINT FK_GOODS_RECEIPT_USER FOREIGN KEY (received_by) REFERENCES dbo.[USER_ACCOUNT](account_id),
        CONSTRAINT CK_GOODS_RECEIPT_status CHECK (status IN ('DRAFT','CONFIRMED','CANCELLED'))
    );
    CREATE INDEX IX_GOODS_RECEIPT_supplier ON dbo.[GOODS_RECEIPT](supplier_id);
END;
GO

IF OBJECT_ID(N'dbo.[GOODS_RECEIPT_ITEM]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[GOODS_RECEIPT_ITEM] (
        receipt_item_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_GOODS_RECEIPT_ITEM PRIMARY KEY,
        receipt_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        location_id BIGINT NOT NULL,
        quantity INT NOT NULL,
        unit_cost DECIMAL(18,2) NOT NULL,
        expiry_date DATE NULL,
        CONSTRAINT FK_GOODS_RECEIPT_ITEM_receipt FOREIGN KEY (receipt_id) REFERENCES dbo.[GOODS_RECEIPT](receipt_id),
        CONSTRAINT FK_GOODS_RECEIPT_ITEM_product FOREIGN KEY (product_id) REFERENCES dbo.[PRODUCT](product_id),
        CONSTRAINT FK_GOODS_RECEIPT_ITEM_location FOREIGN KEY (location_id) REFERENCES dbo.[STORAGE_LOCATION](location_id),
        CONSTRAINT CK_GOODS_RECEIPT_ITEM_qty CHECK (quantity > 0 AND unit_cost >= 0)
    );
    CREATE INDEX IX_GOODS_RECEIPT_ITEM_receipt ON dbo.[GOODS_RECEIPT_ITEM](receipt_id);
END;
GO

IF OBJECT_ID(N'dbo.[GOODS_ISSUE]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[GOODS_ISSUE] (
        issue_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_GOODS_ISSUE PRIMARY KEY,
        issue_no VARCHAR(40) NOT NULL CONSTRAINT UQ_GOODS_ISSUE_no UNIQUE,
        issued_by BIGINT NOT NULL,
        issue_type VARCHAR(25) NOT NULL,
        reference_id BIGINT NULL, -- optional polymorphic reference; not an FK
        status VARCHAR(20) NOT NULL CONSTRAINT DF_GOODS_ISSUE_status DEFAULT ('DRAFT'),
        issued_at DATETIME2(0) NOT NULL CONSTRAINT DF_GOODS_ISSUE_issued DEFAULT SYSUTCDATETIME(),
        note NVARCHAR(500) NULL,
        CONSTRAINT FK_GOODS_ISSUE_USER FOREIGN KEY (issued_by) REFERENCES dbo.[USER_ACCOUNT](account_id),
        CONSTRAINT CK_GOODS_ISSUE_type CHECK (issue_type IN ('DAMAGE','EXPIRED','TRANSFER_OUT','OTHER')),
        CONSTRAINT CK_GOODS_ISSUE_status CHECK (status IN ('DRAFT','CONFIRMED','CANCELLED'))
    );
END;
GO

IF OBJECT_ID(N'dbo.[GOODS_ISSUE_ITEM]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[GOODS_ISSUE_ITEM] (
        issue_item_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_GOODS_ISSUE_ITEM PRIMARY KEY,
        issue_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        location_id BIGINT NOT NULL,
        quantity INT NOT NULL,
        reason NVARCHAR(255) NULL,
        CONSTRAINT FK_GOODS_ISSUE_ITEM_issue FOREIGN KEY (issue_id) REFERENCES dbo.[GOODS_ISSUE](issue_id),
        CONSTRAINT FK_GOODS_ISSUE_ITEM_product FOREIGN KEY (product_id) REFERENCES dbo.[PRODUCT](product_id),
        CONSTRAINT FK_GOODS_ISSUE_ITEM_location FOREIGN KEY (location_id) REFERENCES dbo.[STORAGE_LOCATION](location_id),
        CONSTRAINT CK_GOODS_ISSUE_ITEM_qty CHECK (quantity > 0)
    );
    CREATE INDEX IX_GOODS_ISSUE_ITEM_issue ON dbo.[GOODS_ISSUE_ITEM](issue_id);
END;
GO

IF OBJECT_ID(N'dbo.[STOCK_COUNT]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[STOCK_COUNT] (
        count_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_STOCK_COUNT PRIMARY KEY,
        count_no VARCHAR(40) NOT NULL CONSTRAINT UQ_STOCK_COUNT_no UNIQUE,
        counted_by BIGINT NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_STOCK_COUNT_status DEFAULT ('OPEN'),
        started_at DATETIME2(0) NOT NULL CONSTRAINT DF_STOCK_COUNT_started DEFAULT SYSUTCDATETIME(),
        completed_at DATETIME2(0) NULL,
        note NVARCHAR(500) NULL,
        CONSTRAINT FK_STOCK_COUNT_USER FOREIGN KEY (counted_by) REFERENCES dbo.[USER_ACCOUNT](account_id),
        CONSTRAINT CK_STOCK_COUNT_status CHECK (status IN ('OPEN','COMPLETED','CANCELLED')),
        CONSTRAINT CK_STOCK_COUNT_dates CHECK (completed_at IS NULL OR completed_at >= started_at)
    );
END;
GO

IF OBJECT_ID(N'dbo.[STOCK_COUNT_ITEM]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[STOCK_COUNT_ITEM] (
        count_item_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_STOCK_COUNT_ITEM PRIMARY KEY,
        count_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        location_id BIGINT NOT NULL,
        system_qty INT NOT NULL,
        physical_qty INT NOT NULL,
        variance_qty INT NOT NULL,
        CONSTRAINT UQ_STOCK_COUNT_ITEM_triplet UNIQUE (count_id, product_id, location_id),
        CONSTRAINT FK_STOCK_COUNT_ITEM_count FOREIGN KEY (count_id) REFERENCES dbo.[STOCK_COUNT](count_id),
        CONSTRAINT FK_STOCK_COUNT_ITEM_product FOREIGN KEY (product_id) REFERENCES dbo.[PRODUCT](product_id),
        CONSTRAINT FK_STOCK_COUNT_ITEM_location FOREIGN KEY (location_id) REFERENCES dbo.[STORAGE_LOCATION](location_id),
        CONSTRAINT CK_STOCK_COUNT_ITEM_qty CHECK (system_qty >= 0 AND physical_qty >= 0 AND variance_qty = physical_qty - system_qty)
    );
END;
GO

IF OBJECT_ID(N'dbo.[STOCK_ADJUSTMENT]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[STOCK_ADJUSTMENT] (
        adjustment_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_STOCK_ADJUSTMENT PRIMARY KEY,
        adjustment_no VARCHAR(40) NOT NULL CONSTRAINT UQ_STOCK_ADJUSTMENT_no UNIQUE,
        count_id BIGINT NULL,
        adjusted_by BIGINT NOT NULL,
        reason NVARCHAR(500) NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_STOCK_ADJUSTMENT_status DEFAULT ('DRAFT'),
        adjusted_at DATETIME2(0) NOT NULL CONSTRAINT DF_STOCK_ADJUSTMENT_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_STOCK_ADJUSTMENT_COUNT FOREIGN KEY (count_id) REFERENCES dbo.[STOCK_COUNT](count_id),
        CONSTRAINT FK_STOCK_ADJUSTMENT_USER FOREIGN KEY (adjusted_by) REFERENCES dbo.[USER_ACCOUNT](account_id),
        CONSTRAINT CK_STOCK_ADJUSTMENT_status CHECK (status IN ('DRAFT','APPROVED','APPLIED','REJECTED'))
    );
    CREATE INDEX IX_STOCK_ADJUSTMENT_count ON dbo.[STOCK_ADJUSTMENT](count_id);
END;
GO

IF OBJECT_ID(N'dbo.[STOCK_ADJUSTMENT_ITEM]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[STOCK_ADJUSTMENT_ITEM] (
        adjustment_item_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_STOCK_ADJUSTMENT_ITEM PRIMARY KEY,
        adjustment_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        location_id BIGINT NOT NULL,
        quantity_change INT NOT NULL,
        note NVARCHAR(255) NULL,
        CONSTRAINT FK_STOCK_ADJUSTMENT_ITEM_adjustment FOREIGN KEY (adjustment_id) REFERENCES dbo.[STOCK_ADJUSTMENT](adjustment_id),
        CONSTRAINT FK_STOCK_ADJUSTMENT_ITEM_product FOREIGN KEY (product_id) REFERENCES dbo.[PRODUCT](product_id),
        CONSTRAINT FK_STOCK_ADJUSTMENT_ITEM_location FOREIGN KEY (location_id) REFERENCES dbo.[STORAGE_LOCATION](location_id),
        CONSTRAINT CK_STOCK_ADJUSTMENT_ITEM_nonzero CHECK (quantity_change <> 0)
    );
    CREATE INDEX IX_STOCK_ADJUSTMENT_ITEM_adjustment ON dbo.[STOCK_ADJUSTMENT_ITEM](adjustment_id);
END;
GO

IF OBJECT_ID(N'dbo.[STOCK_TRANSACTION]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[STOCK_TRANSACTION] (
        stock_txn_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_STOCK_TRANSACTION PRIMARY KEY,
        product_id BIGINT NOT NULL,
        location_id BIGINT NOT NULL,
        performed_by BIGINT NOT NULL,
        transaction_type VARCHAR(25) NOT NULL,
        quantity_change INT NOT NULL,
        reference_type VARCHAR(30) NOT NULL,
        reference_id BIGINT NULL, -- document ID; type determines which table
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_STOCK_TRANSACTION_created DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_STOCK_TRANSACTION_PRODUCT FOREIGN KEY (product_id) REFERENCES dbo.[PRODUCT](product_id),
        CONSTRAINT FK_STOCK_TRANSACTION_LOCATION FOREIGN KEY (location_id) REFERENCES dbo.[STORAGE_LOCATION](location_id),
        CONSTRAINT FK_STOCK_TRANSACTION_USER FOREIGN KEY (performed_by) REFERENCES dbo.[USER_ACCOUNT](account_id),
        CONSTRAINT CK_STOCK_TRANSACTION_change CHECK (quantity_change <> 0),
        CONSTRAINT CK_STOCK_TRANSACTION_type CHECK (transaction_type IN ('RECEIPT','ISSUE','SALE','REFUND','ADJUSTMENT')),
        CONSTRAINT CK_STOCK_TRANSACTION_reference CHECK (reference_type IN ('GOODS_RECEIPT','GOODS_ISSUE','SALE','REFUND','STOCK_ADJUSTMENT'))
    );
    CREATE INDEX IX_STOCK_TRANSACTION_product_date ON dbo.[STOCK_TRANSACTION](product_id, created_at DESC);
    CREATE INDEX IX_STOCK_TRANSACTION_reference ON dbo.[STOCK_TRANSACTION](reference_type, reference_id);
END;
GO

IF OBJECT_ID(N'dbo.[LOW_STOCK_ALERT]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[LOW_STOCK_ALERT] (
        alert_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LOW_STOCK_ALERT PRIMARY KEY,
        product_id BIGINT NOT NULL,
        location_id BIGINT NOT NULL,
        current_qty INT NOT NULL,
        reorder_level INT NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_LOW_STOCK_ALERT_status DEFAULT ('ACTIVE'),
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_LOW_STOCK_ALERT_created DEFAULT SYSUTCDATETIME(),
        resolved_at DATETIME2(0) NULL,
        CONSTRAINT FK_LOW_STOCK_ALERT_PRODUCT FOREIGN KEY (product_id) REFERENCES dbo.[PRODUCT](product_id),
        CONSTRAINT FK_LOW_STOCK_ALERT_LOCATION FOREIGN KEY (location_id) REFERENCES dbo.[STORAGE_LOCATION](location_id),
        CONSTRAINT CK_LOW_STOCK_ALERT_qty CHECK (current_qty >= 0 AND reorder_level >= 0),
        CONSTRAINT CK_LOW_STOCK_ALERT_status CHECK (status IN ('ACTIVE','RESOLVED')),
        CONSTRAINT CK_LOW_STOCK_ALERT_resolved CHECK ((status='ACTIVE' AND resolved_at IS NULL) OR (status='RESOLVED' AND resolved_at IS NOT NULL))
    );
    CREATE UNIQUE INDEX UX_LOW_STOCK_ALERT_active ON dbo.[LOW_STOCK_ALERT](product_id, location_id) WHERE status='ACTIVE';
END;
GO

/* 4. Sales, payments and refunds */
IF OBJECT_ID(N'dbo.[SALE]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[SALE] (
        sale_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SALE PRIMARY KEY,
        cashier_id BIGINT NOT NULL,
        sale_no VARCHAR(40) NOT NULL CONSTRAINT UQ_SALE_no UNIQUE,
        status VARCHAR(25) NOT NULL CONSTRAINT DF_SALE_status DEFAULT ('DRAFT'),
        subtotal DECIMAL(18,2) NOT NULL CONSTRAINT DF_SALE_subtotal DEFAULT (0),
        discount_total DECIMAL(18,2) NOT NULL CONSTRAINT DF_SALE_discount DEFAULT (0),
        grand_total DECIMAL(18,2) NOT NULL CONSTRAINT DF_SALE_total DEFAULT (0),
        sold_at DATETIME2(0) NOT NULL CONSTRAINT DF_SALE_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_SALE_USER FOREIGN KEY (cashier_id) REFERENCES dbo.[USER_ACCOUNT](account_id),
        CONSTRAINT CK_SALE_status CHECK (status IN ('DRAFT','PENDING_PAYMENT','COMPLETED','CANCELLED')),
        CONSTRAINT CK_SALE_money CHECK (subtotal >= 0 AND discount_total >= 0 AND discount_total <= subtotal AND grand_total = subtotal - discount_total)
    );
    CREATE INDEX IX_SALE_cashier_date ON dbo.[SALE](cashier_id, sold_at DESC);
END;
GO

IF OBJECT_ID(N'dbo.[SALE_ITEM]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[SALE_ITEM] (
        sale_item_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SALE_ITEM PRIMARY KEY,
        sale_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        location_id BIGINT NOT NULL,
        quantity INT NOT NULL,
        unit_price DECIMAL(18,2) NOT NULL,
        discount_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_SALE_ITEM_discount DEFAULT (0),
        line_total DECIMAL(18,2) NOT NULL,
        CONSTRAINT FK_SALE_ITEM_SALE FOREIGN KEY (sale_id) REFERENCES dbo.[SALE](sale_id),
        CONSTRAINT FK_SALE_ITEM_PRODUCT FOREIGN KEY (product_id) REFERENCES dbo.[PRODUCT](product_id),
        CONSTRAINT FK_SALE_ITEM_LOCATION FOREIGN KEY (location_id) REFERENCES dbo.[STORAGE_LOCATION](location_id),
        CONSTRAINT CK_SALE_ITEM_values CHECK (quantity > 0 AND unit_price >= 0 AND discount_amount >= 0 AND discount_amount <= quantity * unit_price AND line_total = quantity * unit_price - discount_amount)
    );
    CREATE INDEX IX_SALE_ITEM_sale ON dbo.[SALE_ITEM](sale_id);
    CREATE INDEX IX_SALE_ITEM_product ON dbo.[SALE_ITEM](product_id);
END;
GO

IF OBJECT_ID(N'dbo.[PAYMENT]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[PAYMENT] (
        payment_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PAYMENT PRIMARY KEY,
        sale_id BIGINT NOT NULL,
        method VARCHAR(20) NOT NULL,
        provider VARCHAR(30) NULL,
        amount DECIMAL(18,2) NOT NULL,
        status VARCHAR(25) NOT NULL CONSTRAINT DF_PAYMENT_status DEFAULT ('PENDING'),
        transaction_ref VARCHAR(120) NULL,
        paid_at DATETIME2(0) NULL,
        CONSTRAINT FK_PAYMENT_SALE FOREIGN KEY (sale_id) REFERENCES dbo.[SALE](sale_id),
        CONSTRAINT CK_PAYMENT_method CHECK (method IN ('CASH','VNPAY')),
        CONSTRAINT CK_PAYMENT_amount CHECK (amount > 0),
        CONSTRAINT CK_PAYMENT_status CHECK (status IN ('PENDING','PAID','FAILED','CANCELLED')),
        CONSTRAINT CK_PAYMENT_paid_at CHECK ((status='PAID' AND paid_at IS NOT NULL) OR (status<>'PAID' AND paid_at IS NULL))
    );
    CREATE UNIQUE INDEX UX_PAYMENT_transaction_ref ON dbo.[PAYMENT](transaction_ref) WHERE transaction_ref IS NOT NULL;
    CREATE INDEX IX_PAYMENT_sale ON dbo.[PAYMENT](sale_id);
END;
GO

IF OBJECT_ID(N'dbo.[REFUND]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[REFUND] (
        refund_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_REFUND PRIMARY KEY,
        sale_id BIGINT NOT NULL,
        payment_id BIGINT NULL,
        processed_by BIGINT NOT NULL,
        total_amount DECIMAL(18,2) NOT NULL,
        reason NVARCHAR(500) NOT NULL,
        status VARCHAR(25) NOT NULL CONSTRAINT DF_REFUND_status DEFAULT ('PENDING'),
        completed_at DATETIME2(0) NULL,
        CONSTRAINT FK_REFUND_SALE FOREIGN KEY (sale_id) REFERENCES dbo.[SALE](sale_id),
        CONSTRAINT FK_REFUND_PAYMENT FOREIGN KEY (payment_id) REFERENCES dbo.[PAYMENT](payment_id),
        CONSTRAINT FK_REFUND_USER FOREIGN KEY (processed_by) REFERENCES dbo.[USER_ACCOUNT](account_id),
        CONSTRAINT CK_REFUND_amount CHECK (total_amount > 0),
        CONSTRAINT CK_REFUND_status CHECK (status IN ('PENDING','COMPLETED','REJECTED')),
        CONSTRAINT CK_REFUND_completed CHECK ((status='COMPLETED' AND completed_at IS NOT NULL) OR (status<>'COMPLETED' AND completed_at IS NULL))
    );
    CREATE INDEX IX_REFUND_sale ON dbo.[REFUND](sale_id);
    CREATE INDEX IX_REFUND_payment ON dbo.[REFUND](payment_id);
END;
GO

IF OBJECT_ID(N'dbo.[REFUND_ITEM]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[REFUND_ITEM] (
        refund_item_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_REFUND_ITEM PRIMARY KEY,
        refund_id BIGINT NOT NULL,
        sale_item_id BIGINT NOT NULL,
        quantity INT NOT NULL,
        refund_amount DECIMAL(18,2) NOT NULL,
        restock BIT NOT NULL CONSTRAINT DF_REFUND_ITEM_restock DEFAULT (1),
        CONSTRAINT UQ_REFUND_ITEM_pair UNIQUE (refund_id, sale_item_id),
        CONSTRAINT FK_REFUND_ITEM_REFUND FOREIGN KEY (refund_id) REFERENCES dbo.[REFUND](refund_id),
        CONSTRAINT FK_REFUND_ITEM_SALE_ITEM FOREIGN KEY (sale_item_id) REFERENCES dbo.[SALE_ITEM](sale_item_id),
        CONSTRAINT CK_REFUND_ITEM_amount CHECK (quantity > 0 AND refund_amount >= 0)
    );
    CREATE INDEX IX_REFUND_ITEM_sale_item ON dbo.[REFUND_ITEM](sale_item_id);
END;
GO

/* 5. Demo data. Every insert checks a stable business key before inserting.
   Data is intentionally sparse in historical tables but covers the main workflows. */
BEGIN TRY
    BEGIN TRANSACTION;

    INSERT dbo.[ROLE] (role_name, description)
    SELECT v.role_name, v.description
    FROM (VALUES
        ('ADMIN', N'Quản trị tài khoản và phân quyền'),
        ('OWNER', N'Chủ cửa hàng, quản lý và xem báo cáo'),
        ('SALE_STAFF', N'Nhân viên bán hàng và hoàn tiền'),
        ('INVENTORY_STAFF', N'Nhân viên quản lý kho')
    ) AS v(role_name, description)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.[ROLE] r WHERE r.role_name=v.role_name);

    INSERT dbo.[USER_ACCOUNT] (role_id, email, account_name, password_hash, full_name, phone, status)
    SELECT r.role_id, v.email, v.account_name, '!SET_BCRYPT_HASH_VIA_APPLICATION!', v.full_name, v.phone, 'PENDING_PASSWORD'
    FROM (VALUES
        ('ADMIN', 'admin@minimart.example', 'admin', N'Minh Admin', '0900000001'),
        ('OWNER', 'owner@minimart.example', 'owner', N'Lan Owner', '0900000002'),
        ('SALE_STAFF', 'cashier1@minimart.example', 'cashier1', N'Hoa Cashier', '0900000003'),
        ('SALE_STAFF', 'cashier2@minimart.example', 'cashier2', N'Nam Cashier', '0900000004'),
        ('INVENTORY_STAFF', 'inventory@minimart.example', 'inventory', N'Bình INVENTORY', '0900000005')
    ) AS v(role_name,email,account_name,full_name,phone)
    JOIN dbo.[ROLE] r ON r.role_name=v.role_name
    WHERE NOT EXISTS (SELECT 1 FROM dbo.[USER_ACCOUNT] a WHERE a.email=v.email);

    INSERT dbo.[CATEGORY] (category_name, description)
    SELECT v.category_name, v.description
    FROM (VALUES
        (N'Gạo và ngũ cốc', N'Gạo, bột và ngũ cốc'),
        (N'Sữa và đồ uống', N'Sữa và thức uống đóng hộp'),
        (N'Thực phẩm tươi', N'Trứng và thực phẩm tươi'),
        (N'Mì và thực phẩm khô', N'Mì gói, thực phẩm khô'),
        (N'Chăm sóc cá nhân', N'Xà phòng và đồ dùng cá nhân')
    ) AS v(category_name,description)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.[CATEGORY] c WHERE c.category_name=v.category_name);

    INSERT dbo.[SUPPLIER] (supplier_code,supplier_name,email,phone,address)
    SELECT v.code,v.name,v.email,v.phone,v.address
    FROM (VALUES
        ('SUP001',N'Nhà cung cấp Gạo Việt','gao@example.com','0281111001',N'TP. Hồ Chí Minh'),
        ('SUP002',N'Nhà cung cấp Sữa Tươi','sua@example.com','0281111002',N'Bình Dương'),
        ('SUP003',N'Nhà cung cấp Trứng Sạch','trung@example.com','0281111003',N'Đồng Nai'),
        ('SUP004',N'Nhà cung cấp Mì Việt','mi@example.com','0281111004',N'Long An'),
        ('SUP005',N'Nhà cung cấp Hàng Gia Dụng','giadung@example.com','0281111005',N'TP. Hồ Chí Minh')
    ) AS v(code,name,email,phone,address)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.[SUPPLIER] s WHERE s.supplier_code=v.code);

    INSERT dbo.[STORAGE_LOCATION] (location_code,location_name,description)
    SELECT v.code,v.name,v.description
    FROM (VALUES
        ('LOC-A1',N'Kệ gạo A1',N'Khu thực phẩm khô'),
        ('LOC-B1',N'Kệ sữa B1',N'Khu đồ uống'),
        ('LOC-C1',N'Kệ trứng C1',N'Khu thực phẩm tươi'),
        ('LOC-D1',N'Kệ mì D1',N'Khu thực phẩm khô'),
        ('LOC-E1',N'Kệ chăm sóc E1',N'Khu đồ gia dụng')
    ) AS v(code,name,description)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.[STORAGE_LOCATION] l WHERE l.location_code=v.code);

    INSERT dbo.[PRODUCT] (category_id,supplier_id,sku,barcode,product_name,cost_price,selling_price,unit,reorder_level)
    SELECT c.category_id,s.supplier_id,v.sku,v.barcode,v.name,v.cost,v.price,v.unit,v.threshold
    FROM (VALUES
        (N'Gạo và ngũ cốc','SUP001','SKU-RICE-001','8930000000011',N'Gạo trắng 1 kg',20000.00,28000.00,'bag',10),
        (N'Sữa và đồ uống','SUP002','SKU-MILK-001','8930000000012',N'Sữa tươi 180 ml',10000.00,15000.00,'box',15),
        (N'Thực phẩm tươi','SUP003','SKU-EGGS-001','8930000000013',N'Hộp 10 quả trứng',23000.00,32000.00,'box',12),
        (N'Mì và thực phẩm khô','SUP004','SKU-NOODLE-001','8930000000014',N'Mì gói vị bò',3500.00,5000.00,'pack',25),
        (N'Chăm sóc cá nhân','SUP005','SKU-SOAP-001','8930000000015',N'Xà phòng 90 g',9000.00,14000.00,'bar',25)
    ) AS v(category_name,supplier_code,sku,barcode,name,cost,price,unit,threshold)
    JOIN dbo.[CATEGORY] c ON c.category_name=v.category_name
    JOIN dbo.[SUPPLIER] s ON s.supplier_code=v.supplier_code
    WHERE NOT EXISTS (SELECT 1 FROM dbo.[PRODUCT] p WHERE p.sku=v.sku);

    /* Final balances: rice 50 - 2 + 1 = 49; milk 40 - 1 = 39;
       eggs 30 - 1 adjustment = 29; noodles 100 - 1 issue = 99; soap 20. */
    INSERT dbo.[INVENTORY] (product_id,location_id,quantity_on_hand,quantity_reserved,last_counted_at)
    SELECT p.product_id,l.location_id,v.on_hand,0,
           CASE WHEN v.sku='SKU-EGGS-001' THEN SYSUTCDATETIME() ELSE NULL END
    FROM (VALUES
        ('SKU-RICE-001','LOC-A1',49),('SKU-MILK-001','LOC-B1',39),
        ('SKU-EGGS-001','LOC-C1',29),('SKU-NOODLE-001','LOC-D1',99),
        ('SKU-SOAP-001','LOC-E1',20)
    ) AS v(sku,code,on_hand)
    JOIN dbo.[PRODUCT] p ON p.sku=v.sku
    JOIN dbo.[STORAGE_LOCATION] l ON l.location_code=v.code
    WHERE NOT EXISTS (SELECT 1 FROM dbo.[INVENTORY] i WHERE i.product_id=p.product_id AND i.location_id=l.location_id);

    DECLARE @inventoryId BIGINT=(SELECT account_id FROM dbo.[USER_ACCOUNT] WHERE email='inventory@minimart.example');
    DECLARE @cashierId BIGINT=(SELECT account_id FROM dbo.[USER_ACCOUNT] WHERE email='cashier1@minimart.example');

    IF NOT EXISTS (SELECT 1 FROM dbo.[GOODS_RECEIPT] WHERE receipt_no='GR-DEMO-001')
        INSERT dbo.[GOODS_RECEIPT] (receipt_no,supplier_id,received_by,status,note)
        VALUES ('GR-DEMO-001',NULL,@inventoryId,'CONFIRMED',N'Opening inventory: aggregate initial stock from multiple suppliers');
    DECLARE @receiptId BIGINT=(SELECT receipt_id FROM dbo.[GOODS_RECEIPT] WHERE receipt_no='GR-DEMO-001');

    INSERT dbo.[GOODS_RECEIPT_ITEM] (receipt_id,product_id,location_id,quantity,unit_cost)
    SELECT @receiptId,p.product_id,l.location_id,v.quantity,v.unit_cost
    FROM (VALUES
        ('SKU-RICE-001','LOC-A1',50,20000.00),('SKU-MILK-001','LOC-B1',40,10000.00),
        ('SKU-EGGS-001','LOC-C1',30,23000.00),('SKU-NOODLE-001','LOC-D1',100,3500.00),
        ('SKU-SOAP-001','LOC-E1',20,9000.00)
    ) AS v(sku,code,quantity,unit_cost)
    JOIN dbo.[PRODUCT] p ON p.sku=v.sku JOIN dbo.[STORAGE_LOCATION] l ON l.location_code=v.code
    WHERE NOT EXISTS (SELECT 1 FROM dbo.[GOODS_RECEIPT_ITEM] x WHERE x.receipt_id=@receiptId AND x.product_id=p.product_id AND x.location_id=l.location_id);

    IF NOT EXISTS (SELECT 1 FROM dbo.[GOODS_ISSUE] WHERE issue_no='GI-DEMO-001')
        INSERT dbo.[GOODS_ISSUE] (issue_no,issued_by,issue_type,status,note)
        VALUES ('GI-DEMO-001',@inventoryId,'DAMAGE','CONFIRMED',N'Xuất hủy một gói mì bị hỏng');
    DECLARE @issueId BIGINT=(SELECT issue_id FROM dbo.[GOODS_ISSUE] WHERE issue_no='GI-DEMO-001');
    INSERT dbo.[GOODS_ISSUE_ITEM] (issue_id,product_id,location_id,quantity,reason)
    SELECT @issueId,p.product_id,l.location_id,1,N'Bao bì rách'
    FROM dbo.[PRODUCT] p CROSS JOIN dbo.[STORAGE_LOCATION] l
    WHERE p.sku='SKU-NOODLE-001' AND l.location_code='LOC-D1'
      AND NOT EXISTS (SELECT 1 FROM dbo.[GOODS_ISSUE_ITEM] x WHERE x.issue_id=@issueId AND x.product_id=p.product_id);

    IF NOT EXISTS (SELECT 1 FROM dbo.[SALE] WHERE sale_no='SALE-DEMO-001')
        INSERT dbo.[SALE] (cashier_id,sale_no,status,subtotal,discount_total,grand_total)
        VALUES (@cashierId,'SALE-DEMO-001','COMPLETED',71000.00,0,71000.00);
    DECLARE @saleId BIGINT=(SELECT sale_id FROM dbo.[SALE] WHERE sale_no='SALE-DEMO-001');
    INSERT dbo.[SALE_ITEM] (sale_id,product_id,location_id,quantity,unit_price,discount_amount,line_total)
    SELECT @saleId,p.product_id,l.location_id,v.quantity,v.unit_price,0,v.quantity*v.unit_price
    FROM (VALUES ('SKU-RICE-001','LOC-A1',2,28000.00),('SKU-MILK-001','LOC-B1',1,15000.00)) AS v(sku,code,quantity,unit_price)
    JOIN dbo.[PRODUCT] p ON p.sku=v.sku JOIN dbo.[STORAGE_LOCATION] l ON l.location_code=v.code
    WHERE NOT EXISTS (SELECT 1 FROM dbo.[SALE_ITEM] x WHERE x.sale_id=@saleId AND x.product_id=p.product_id);

    IF NOT EXISTS (SELECT 1 FROM dbo.[PAYMENT] WHERE transaction_ref='CASH-DEMO-001')
        INSERT dbo.[PAYMENT] (sale_id,method,provider,amount,status,transaction_ref,paid_at)
        VALUES (@saleId,'CASH',NULL,71000.00,'PAID','CASH-DEMO-001',SYSUTCDATETIME());
    DECLARE @paymentId BIGINT=(SELECT payment_id FROM dbo.[PAYMENT] WHERE transaction_ref='CASH-DEMO-001');

    IF NOT EXISTS (SELECT 1 FROM dbo.[REFUND] WHERE sale_id=@saleId AND reason=N'Khách trả lại một túi gạo — dữ liệu mẫu')
        INSERT dbo.[REFUND] (sale_id,payment_id,processed_by,total_amount,reason,status,completed_at)
        VALUES (@saleId,@paymentId,@cashierId,28000.00,N'Khách trả lại một túi gạo — dữ liệu mẫu','COMPLETED',SYSUTCDATETIME());
    DECLARE @refundId BIGINT=(SELECT TOP(1) refund_id FROM dbo.[REFUND]
                              WHERE sale_id=@saleId AND reason=N'Khách trả lại một túi gạo — dữ liệu mẫu' ORDER BY refund_id);
    INSERT dbo.[REFUND_ITEM] (refund_id,sale_item_id,quantity,refund_amount,restock)
    SELECT @refundId,si.sale_item_id,1,28000.00,1
    FROM dbo.[SALE_ITEM] si JOIN dbo.[PRODUCT] p ON p.product_id=si.product_id
    WHERE si.sale_id=@saleId AND p.sku='SKU-RICE-001'
      AND NOT EXISTS (SELECT 1 FROM dbo.[REFUND_ITEM] x WHERE x.refund_id=@refundId AND x.sale_item_id=si.sale_item_id);

    IF NOT EXISTS (SELECT 1 FROM dbo.[STOCK_COUNT] WHERE count_no='COUNT-DEMO-001')
        INSERT dbo.[STOCK_COUNT] (count_no,counted_by,status,started_at,completed_at,note)
        VALUES ('COUNT-DEMO-001',@inventoryId,'COMPLETED',DATEADD(MINUTE,-10,SYSUTCDATETIME()),SYSUTCDATETIME(),N'Kiểm kê 5 mặt hàng mẫu');
    DECLARE @countId BIGINT=(SELECT count_id FROM dbo.[STOCK_COUNT] WHERE count_no='COUNT-DEMO-001');
    INSERT dbo.[STOCK_COUNT_ITEM] (count_id,product_id,location_id,system_qty,physical_qty,variance_qty)
    SELECT @countId,p.product_id,l.location_id,v.system_qty,v.physical_qty,v.physical_qty-v.system_qty
    FROM (VALUES
        ('SKU-RICE-001','LOC-A1',49,49),('SKU-MILK-001','LOC-B1',39,39),
        ('SKU-EGGS-001','LOC-C1',30,29),('SKU-NOODLE-001','LOC-D1',99,99),
        ('SKU-SOAP-001','LOC-E1',20,20)
    ) AS v(sku,code,system_qty,physical_qty)
    JOIN dbo.[PRODUCT] p ON p.sku=v.sku JOIN dbo.[STORAGE_LOCATION] l ON l.location_code=v.code
    WHERE NOT EXISTS (SELECT 1 FROM dbo.[STOCK_COUNT_ITEM] x WHERE x.count_id=@countId AND x.product_id=p.product_id AND x.location_id=l.location_id);

    IF NOT EXISTS (SELECT 1 FROM dbo.[STOCK_ADJUSTMENT] WHERE adjustment_no='ADJ-DEMO-001')
        INSERT dbo.[STOCK_ADJUSTMENT] (adjustment_no,count_id,adjusted_by,reason,status)
        VALUES ('ADJ-DEMO-001',@countId,@inventoryId,N'Trứng kiểm kê thiếu một hộp','APPLIED');
    DECLARE @adjustmentId BIGINT=(SELECT adjustment_id FROM dbo.[STOCK_ADJUSTMENT] WHERE adjustment_no='ADJ-DEMO-001');
    INSERT dbo.[STOCK_ADJUSTMENT_ITEM] (adjustment_id,product_id,location_id,quantity_change,note)
    SELECT @adjustmentId,p.product_id,l.location_id,-1,N'Variance 30 -> 29'
    FROM dbo.[PRODUCT] p CROSS JOIN dbo.[STORAGE_LOCATION] l
    WHERE p.sku='SKU-EGGS-001' AND l.location_code='LOC-C1'
      AND NOT EXISTS (SELECT 1 FROM dbo.[STOCK_ADJUSTMENT_ITEM] x WHERE x.adjustment_id=@adjustmentId AND x.product_id=p.product_id);

    /* Transaction history: one row per document/product/location movement. */
    INSERT dbo.[STOCK_TRANSACTION] (product_id,location_id,performed_by,transaction_type,quantity_change,reference_type,reference_id)
    SELECT ri.product_id,ri.location_id,@inventoryId,'RECEIPT',ri.quantity,'GOODS_RECEIPT',@receiptId
    FROM dbo.[GOODS_RECEIPT_ITEM] ri WHERE ri.receipt_id=@receiptId
      AND NOT EXISTS (SELECT 1 FROM dbo.[STOCK_TRANSACTION] x WHERE x.reference_type='GOODS_RECEIPT' AND x.reference_id=@receiptId AND x.product_id=ri.product_id AND x.location_id=ri.location_id);

    INSERT dbo.[STOCK_TRANSACTION] (product_id,location_id,performed_by,transaction_type,quantity_change,reference_type,reference_id)
    SELECT ii.product_id,ii.location_id,@inventoryId,'ISSUE',-ii.quantity,'GOODS_ISSUE',@issueId
    FROM dbo.[GOODS_ISSUE_ITEM] ii WHERE ii.issue_id=@issueId
      AND NOT EXISTS (SELECT 1 FROM dbo.[STOCK_TRANSACTION] x WHERE x.reference_type='GOODS_ISSUE' AND x.reference_id=@issueId AND x.product_id=ii.product_id AND x.location_id=ii.location_id);

    INSERT dbo.[STOCK_TRANSACTION] (product_id,location_id,performed_by,transaction_type,quantity_change,reference_type,reference_id)
    SELECT si.product_id,si.location_id,@cashierId,'SALE',-si.quantity,'SALE',@saleId
    FROM dbo.[SALE_ITEM] si WHERE si.sale_id=@saleId
      AND NOT EXISTS (SELECT 1 FROM dbo.[STOCK_TRANSACTION] x WHERE x.reference_type='SALE' AND x.reference_id=@saleId AND x.product_id=si.product_id AND x.location_id=si.location_id);

    INSERT dbo.[STOCK_TRANSACTION] (product_id,location_id,performed_by,transaction_type,quantity_change,reference_type,reference_id)
    SELECT si.product_id,si.location_id,@cashierId,'REFUND',ri.quantity,'REFUND',@refundId
    FROM dbo.[REFUND_ITEM] ri JOIN dbo.[SALE_ITEM] si ON si.sale_item_id=ri.sale_item_id
    WHERE ri.refund_id=@refundId AND ri.restock=1
      AND NOT EXISTS (SELECT 1 FROM dbo.[STOCK_TRANSACTION] x WHERE x.reference_type='REFUND' AND x.reference_id=@refundId AND x.product_id=si.product_id AND x.location_id=si.location_id);

    INSERT dbo.[STOCK_TRANSACTION] (product_id,location_id,performed_by,transaction_type,quantity_change,reference_type,reference_id)
    SELECT ai.product_id,ai.location_id,@inventoryId,'ADJUSTMENT',ai.quantity_change,'STOCK_ADJUSTMENT',@adjustmentId
    FROM dbo.[STOCK_ADJUSTMENT_ITEM] ai WHERE ai.adjustment_id=@adjustmentId
      AND NOT EXISTS (SELECT 1 FROM dbo.[STOCK_TRANSACTION] x WHERE x.reference_type='STOCK_ADJUSTMENT' AND x.reference_id=@adjustmentId AND x.product_id=ai.product_id AND x.location_id=ai.location_id);

    INSERT dbo.[LOW_STOCK_ALERT] (product_id,location_id,current_qty,reorder_level,status)
    SELECT i.product_id,i.location_id,i.quantity_on_hand,p.reorder_level,'ACTIVE'
    FROM dbo.[INVENTORY] i JOIN dbo.[PRODUCT] p ON p.product_id=i.product_id
    WHERE p.sku='SKU-SOAP-001' AND i.quantity_on_hand < p.reorder_level
      AND NOT EXISTS (SELECT 1 FROM dbo.[LOW_STOCK_ALERT] a WHERE a.product_id=i.product_id AND a.location_id=i.location_id AND a.status='ACTIVE');

    IF NOT EXISTS (SELECT 1 FROM dbo.[AUDIT_LOG] WHERE action='DEMO_SEED' AND entity_name='DATABASE')
        INSERT dbo.[AUDIT_LOG] (actor_id,action,entity_name,entity_id,new_values)
        VALUES (NULL,'DEMO_SEED','DATABASE','MiniMartSWD392',N'{"scenario":"receipt, issue, count, sale, payment, refund, low-stock"}');

    COMMIT TRANSACTION;
    PRINT N'MiniMartSWD392: schema and sample data are ready.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* 6. Verification queries (read-only) */
SELECT t.name AS table_name, SUM(p.rows) AS row_count
FROM sys.tables t
JOIN sys.partitions p ON p.object_id=t.object_id AND p.index_id IN (0,1)
WHERE t.schema_id=SCHEMA_ID(N'dbo')
GROUP BY t.name ORDER BY t.name;

SELECT p.sku, p.product_name, l.location_code, i.quantity_on_hand,
       i.quantity_reserved, i.quantity_on_hand-i.quantity_reserved AS available_qty,
       p.reorder_level
FROM dbo.[INVENTORY] i
JOIN dbo.[PRODUCT] p ON p.product_id=i.product_id
JOIN dbo.[STORAGE_LOCATION] l ON l.location_id=i.location_id
ORDER BY p.sku;

SELECT s.sale_no, s.grand_total, pm.method, pm.status AS payment_status,
       COALESCE(SUM(r.total_amount),0) AS refunded_amount
FROM dbo.[SALE] s
LEFT JOIN dbo.[PAYMENT] pm ON pm.sale_id=s.sale_id AND pm.status='PAID'
LEFT JOIN dbo.[REFUND] r ON r.sale_id=s.sale_id AND r.status='COMPLETED'
WHERE s.sale_no='SALE-DEMO-001'
GROUP BY s.sale_no,s.grand_total,pm.method,pm.status;

SELECT p.sku,l.location_code,a.current_qty,a.reorder_level,a.status
FROM dbo.[LOW_STOCK_ALERT] a
JOIN dbo.[PRODUCT] p ON p.product_id=a.product_id
JOIN dbo.[STORAGE_LOCATION] l ON l.location_id=a.location_id
WHERE a.status='ACTIVE';
GO
