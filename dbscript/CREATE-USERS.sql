-- USERS
CREATE TABLE USERS (
                       ID                 NUMBER PRIMARY KEY,
                       USERNAME           VARCHAR2(64) NOT NULL UNIQUE,
                       PASSWORD_HASH      VARCHAR2(100) NOT NULL, -- BCrypt ~60 chars
                       EMAIL              VARCHAR2(255),
                       ROLE               VARCHAR2(50) DEFAULT 'USER' NOT NULL,
                       STATUS             VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL, -- ACTIVE/LOCKED
                       CREATED_AT         TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
                       UPDATED_AT         TIMESTAMP,
                       LAST_LOGIN_AT      TIMESTAMP
);

CREATE SEQUENCE USERS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
