-- 1. users (2000명)
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
FROM generate_series(1, 2000) AS i
ON CONFLICT (email) DO NOTHING;

-- 데이터 삽입이 끝난 후, 시퀀스 번호를 동기화
SELECT setval('users_seq', (SELECT MAX(user_id) FROM users));


-- 2. workspaces (400개)
-- 사용자 5명당 워크스페이스 1개, 첫 번째 사용자가 owner
INSERT INTO workspaces (workspace_id, public_id, name, owner_id, created_at, updated_at, created_by, updated_by)
SELECT
    ws_idx,
    gen_random_uuid(),
    '부하테스트-워크스페이스-' || ws_idx,
    (SELECT user_id FROM users WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + 1) || '@letzcollab.xyz'),
    NOW(),
    NOW(),
    (SELECT public_id FROM users WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + 1) || '@letzcollab.xyz'),
    (SELECT public_id FROM users WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + 1) || '@letzcollab.xyz')
FROM generate_series(1, 400) AS ws_idx
ON CONFLICT DO NOTHING;

SELECT setval('workspaces_seq', (SELECT MAX(workspace_id) FROM workspaces));

-- 3. workspace_members (1 ~ 2000)
-- 각 워크스페이스에 5명 등록, 첫 번째는 OWNER 나머지는 MEMBER
INSERT INTO workspace_members (workspace_member_id, workspace_id, member_id, position, role, created_at)
SELECT
    ROW_NUMBER() OVER () AS workspace_member_id,
    ws_idx AS workspace_id,
    (SELECT user_id FROM users WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + member_offset) || '@letzcollab.xyz'),
    '부하테스트-포지션',
    CASE WHEN member_offset = 1 THEN 'OWNER' ELSE 'MEMBER' END,
    NOW()
FROM generate_series(1, 400) AS ws_idx
CROSS JOIN generate_series(1, 5) AS member_offset
ON CONFLICT DO NOTHING;

SELECT setval('workspace_members_seq', (SELECT MAX(workspace_member_id) FROM workspace_members));


-- 3. projects (1 ~ 10000)
-- 각 워크스페이스당 25개의 프로젝트 생성, 리더는 워크스페이스 OWNER(첫 번째 유저)로 고정
INSERT INTO projects (
    project_id, public_id, workspace_id, name, description, status, start_date, end_date, is_private, lead_id,
    created_at, updated_at, created_by, updated_by
)
SELECT
    ROW_NUMBER() OVER () AS project_id,
    gen_random_uuid(),
    ws_idx AS workspace_id,
    '프로젝트-WS' || ws_idx || '-' || proj_idx,
    '부하 테스트용 프로젝트',
    (ARRAY['PLANNED','ACTIVE','ON_HOLD','COMPLETED','ARCHIVED'])[(proj_idx % 5) + 1],
    CURRENT_DATE - 30,
    CURRENT_DATE + 60,
    false,
    (SELECT user_id FROM users
      WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + 1) || '@letzcollab.xyz'),
    NOW(),
    NOW(),
    (SELECT public_id FROM users
      WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + 1) || '@letzcollab.xyz'),
    (SELECT public_id FROM users
      WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + 1) || '@letzcollab.xyz')
FROM generate_series(1, 400)  AS ws_idx
CROSS JOIN generate_series(1, 25) AS proj_idx
ON CONFLICT DO NOTHING;

SELECT setval('projects_seq', (SELECT MAX(project_id) FROM projects));


-- 4. project_members (1 ~ 50000)
-- 각 워크스페이스(400)에 있는 모든 멤버(5)가 모든 프로젝트(25)에 참가
INSERT INTO project_members (project_member_id, project_id, member_id, role, position, created_at)
SELECT
    ROW_NUMBER() OVER () AS project_member_id,
    (ws_idx - 1) * 25 + proj_idx AS project_id,
    (SELECT user_id FROM users
     WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + member_offset) || '@letzcollab.xyz'),
    CASE
        WHEN member_offset = 1 THEN 'ADMIN'
        ELSE 'MEMBER'
    END,
    '부하테스트-역할',
    NOW()
FROM generate_series(1, 400)  AS ws_idx
CROSS JOIN generate_series(1, 25) AS proj_idx
CROSS JOIN generate_series(1, 5)  AS member_offset
ON CONFLICT DO NOTHING;

SELECT setval('project_members_seq', (SELECT MAX(project_member_id) FROM project_members));


-- 5. tasks (1 ~ 250000)
-- 각 프로젝트(10000개)당 25개의 업무 생성
INSERT INTO tasks (
    task_id, public_id, project_id, name, description, status, assignee_id, priority, reporter_id, due_date,
    created_at, updated_at, created_by, updated_by
)
SELECT
    ROW_NUMBER() OVER () AS task_id,
    gen_random_uuid(),
    (ws_idx - 1) * 25 + proj_idx AS project_id,
    '업무-WS' || ws_idx || '-P' || proj_idx || '-T' || task_idx,
    '부하 테스트용 업무',
    (ARRAY['TODO','IN_PROGRESS','IN_REVIEW','DONE', 'CANCELLED'])[(task_idx % 5) + 1],
    (SELECT user_id FROM users
      WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + ((task_idx - 1) % 5) + 1) || '@letzcollab.xyz'),
    (ARRAY['LOW','MEDIUM','HIGH','URGENT'])[(task_idx % 4) + 1],
    (SELECT user_id FROM users
      WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + (task_idx % 5) + 1) || '@letzcollab.xyz'),
    CURRENT_DATE + (task_idx * 3),
    NOW(),
    NOW(),
    (SELECT public_id FROM users
      WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + (task_idx % 5) + 1) || '@letzcollab.xyz'),
    (SELECT public_id FROM users
      WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + (task_idx % 5) + 1) || '@letzcollab.xyz')
FROM generate_series(1, 400)  AS ws_idx
CROSS JOIN generate_series(1, 25) AS proj_idx
CROSS JOIN generate_series(1, 25) AS task_idx
ON CONFLICT DO NOTHING;

SELECT setval('tasks_seq', (SELECT MAX(task_id) FROM tasks));


-- 6. task_comments (1 ~ 1250000)
-- 각 업무(250000개)에 댓글 5개 생성
INSERT INTO task_comments (comment_id, task_id, author_id, content, created_at, updated_at)
SELECT
    ROW_NUMBER() OVER () AS comment_id,
    -- task_id: (ws_idx-1)*(프로젝트수)*(업무수) + (proj_idx-1)*(업무수) + task_idx
    (ws_idx - 1) * 625 + (proj_idx - 1) * 25 + task_idx AS task_id,
    (SELECT user_id FROM users
     WHERE email = 'loadtest' || ((ws_idx - 1) * 5 + ((comment_idx - 1) % 5) + 1) || '@letzcollab.xyz'),
    '부하 테스트 댓글 #' || comment_idx,
    NOW(),
    NOW()
FROM generate_series(1, 400)  AS ws_idx
CROSS JOIN generate_series(1, 25) AS proj_idx
CROSS JOIN generate_series(1, 25) AS task_idx
CROSS JOIN generate_series(1, 5) AS comment_idx;

SELECT setval('task_comments_seq', (SELECT MAX(comment_id) FROM task_comments));

