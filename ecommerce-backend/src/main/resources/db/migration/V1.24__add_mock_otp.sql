-- Fixed OTP for local/testing. One row applies to every mobile number.
-- Integration tests truncate this table so real SMS + random OTP still run.
CREATE TABLE mock_otp (
    id   BIGINT      NOT NULL,
    code VARCHAR(16) NOT NULL,
    CONSTRAINT pk_mock_otp PRIMARY KEY (id)
);

INSERT INTO mock_otp (id, code) VALUES (1, '123456');
