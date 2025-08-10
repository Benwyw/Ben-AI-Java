-- USER_REFRESH_TOKENS (allowlist of refresh tokens, revocable by jti)
CREATE TABLE USER_REFRESH_TOKENS (
                                     ID                 NUMBER PRIMARY KEY,
                                     USER_ID            NUMBER NOT NULL,
                                     JTI                VARCHAR2(64) NOT NULL,              -- JWT ID from refresh token
                                     TOKEN_HASH         VARCHAR2(128) NOT NULL,             -- SHA-256 of refresh token string (Base64/hex)
                                     EXPIRES_AT         TIMESTAMP NOT NULL,
                                     REVOKED            CHAR(1) DEFAULT 'N' CHECK (REVOKED IN ('Y','N')),
                                     CREATED_AT         TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
                                     CONSTRAINT FK_URT_USER FOREIGN KEY (USER_ID) REFERENCES USERS(ID),
                                     CONSTRAINT UX_URT_JTI UNIQUE (JTI)
);

CREATE SEQUENCE USER_REFRESH_TOKENS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX IX_URT_USER_EXPIRES ON USER_REFRESH_TOKENS (USER_ID, EXPIRES_AT);
