-- [루프로 200명 삽입]
-- 비번: Password123!

INSERT INTO users (user_id, public_id, name, email, password, phone_number, status, role, created_at, updated_at)
SELECT
    i,
    gen_random_uuid(),
    '부하테스트유저' || i,
    'loadtest' || i || '@letzcollab.xyz',
    '$2a$10$lUNGu6Ap4IFcBMJ0xDPaHem7pBQE7SttPWaT4mvQF75TxOaRsT0cK',
    '010-0000-' || LPAD(i::text, 4, '0'),
    'ACTIVE',
    'USER',
    NOW(),
    NOW()
FROM generate_series(1, 200) AS i
ON CONFLICT (email) DO NOTHING;

-- 데이터 삽입이 끝난 후, 시퀀스 번호를 동기화
SELECT setval('users_seq', (SELECT MAX(user_id) FROM users));