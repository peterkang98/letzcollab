-- 1. users (1,000,000명)
-- 비번: Password123!
INSERT INTO users (user_id, public_id, name, email, password, phone_number, status, role, created_at, updated_at)
SELECT
    i,
    gen_random_uuid(),
    '부하테스트유저' || i,
    'loadtest' || i || '@letzcollab.xyz',
    '$2a$10$lUNGu6Ap4IFcBMJ0xDPaHem7pBQE7SttPWaT4mvQF75TxOaRsT0cK',
    '010-' || LPAD(((i / 10000) % 10000)::text, 4, '0') || '-' || LPAD((i % 10000)::text, 4, '0'),
    'ACTIVE',
    'USER',
    NOW(),
    NOW()
FROM generate_series(1, 1000000) AS i
ON CONFLICT (email) DO NOTHING;

-- 데이터 삽입이 끝난 후, 시퀀스 번호를 동기화
SELECT setval('users_seq', (SELECT MAX(user_id) FROM users));


-- 2. workspaces (250,000개)
-- 사용자 4명당 워크스페이스 1개, 첫 번째 사용자가 owner
INSERT INTO workspaces (workspace_id, public_id, name, owner_id, created_at, updated_at, created_by, updated_by)
SELECT
    ws_idx,
    gen_random_uuid(),
    '부하테스트-워크스페이스-' || ws_idx,
    u.user_id,
    NOW(), NOW(),
    u.public_id,
    u.public_id
FROM generate_series(1, 250000) AS ws_idx
JOIN users u ON u.email = 'loadtest' || ((ws_idx - 1) * 4 + 1) || '@letzcollab.xyz'
ON CONFLICT DO NOTHING;

SELECT setval('workspaces_seq', (SELECT MAX(workspace_id) FROM workspaces));

-- 3. workspace_members (1 ~ 1,000,000)
-- 각 워크스페이스에 4명씩 등록, 첫 번째는 OWNER 나머지는 MEMBER
INSERT INTO workspace_members (workspace_member_id, workspace_id, member_id, position, role, created_at)
SELECT
    ROW_NUMBER() OVER () AS workspace_member_id,
    w.workspace_id,
    u.user_id,
    '부하테스트-포지션',
    CASE WHEN member_offset = 1 THEN 'OWNER' ELSE 'MEMBER' END,
    NOW()
FROM generate_series(1, 250000) AS ws_idx
CROSS JOIN generate_series(1, 4) AS member_offset
JOIN workspaces w ON w.workspace_id = ws_idx
JOIN users u ON u.email = 'loadtest' || ((ws_idx - 1) * 4 + member_offset) || '@letzcollab.xyz'
ON CONFLICT DO NOTHING;

SELECT setval('workspace_members_seq', (SELECT MAX(workspace_member_id) FROM workspace_members));


-- 4. projects (1 ~ 750,000)
-- 각 워크스페이스당 3개의 프로젝트 생성, 리더는 워크스페이스 OWNER(첫 번째 유저)로 고정
INSERT INTO projects (
    project_id, public_id, workspace_id, name, description, status, start_date, end_date, is_private, lead_id,
    created_at, updated_at, created_by, updated_by
)
SELECT
    ROW_NUMBER() OVER () AS project_id,
    gen_random_uuid(),
    w.workspace_id,
    '프로젝트-WS' || ws_idx || '-' || proj_idx,
    '부하 테스트용 프로젝트',
    (ARRAY['PLANNED','ACTIVE','ON_HOLD','COMPLETED','ARCHIVED'])[(proj_idx % 5) + 1],
    CURRENT_DATE - 30,
    CURRENT_DATE + 60,
    false,
    u.user_id,
    NOW(), NOW(),
    u.public_id,
    u.public_id
FROM generate_series(1, 250000) AS ws_idx
CROSS JOIN generate_series(1, 3) AS proj_idx
JOIN workspaces w ON w.workspace_id = ws_idx
JOIN users u ON u.email = 'loadtest' || ((ws_idx - 1) * 4 + 1) || '@letzcollab.xyz'
ON CONFLICT DO NOTHING;

SELECT setval('projects_seq', (SELECT MAX(project_id) FROM projects));


-- 5. project_members (1 ~ 3,000,000)
-- 각 워크스페이스(250000)에 있는 모든 멤버(4)가 모든 프로젝트(3)에 참가
INSERT INTO project_members (project_member_id, project_id, member_id, role, position, created_at)
SELECT
    ROW_NUMBER() OVER () AS project_member_id,
    p.project_id,
    u.user_id,
    CASE WHEN member_offset = 1 THEN 'ADMIN' ELSE 'MEMBER' END,
    '부하테스트-역할',
    NOW()
FROM generate_series(1, 250000) AS ws_idx
CROSS JOIN generate_series(1, 3) AS proj_idx
CROSS JOIN generate_series(1, 4) AS member_offset
JOIN projects p ON p.name = '프로젝트-WS' || ws_idx || '-' || proj_idx
JOIN users u ON u.email = 'loadtest' || ((ws_idx - 1) * 4 + member_offset) || '@letzcollab.xyz'
ON CONFLICT DO NOTHING;

SELECT setval('project_members_seq', (SELECT MAX(project_member_id) FROM project_members));


-- 6. tasks (1 ~ 2,250,000)
-- 각 프로젝트(750,000개)당 3개의 업무 생성
INSERT INTO tasks (
    task_id, public_id, project_id, name, description, status, assignee_id, priority, reporter_id, due_date,
    created_at, updated_at, created_by, updated_by
)
SELECT
    ROW_NUMBER() OVER () AS task_id,
    gen_random_uuid(),
    p.project_id,
    '업무-WS' || ws_idx || '-P' || proj_idx || '-T' || task_idx,
    '부하 테스트용 업무',
    (ARRAY['TODO','IN_PROGRESS','IN_REVIEW','DONE','CANCELLED'])[(task_idx % 5) + 1],
    assignee.user_id,
    (ARRAY['LOW','MEDIUM','HIGH','URGENT'])[(task_idx % 4) + 1],
    reporter.user_id,
    CURRENT_DATE + (task_idx * 3),
    NOW(), NOW(),
    reporter.public_id,
    reporter.public_id
FROM generate_series(1, 250000) AS ws_idx
CROSS JOIN generate_series(1, 3) AS proj_idx
CROSS JOIN generate_series(1, 3) AS task_idx
JOIN projects p ON p.name = '프로젝트-WS' || ws_idx || '-' || proj_idx
JOIN users assignee ON assignee.email = 'loadtest' || ((ws_idx - 1) * 4 + ((task_idx - 1) % 4) + 1) || '@letzcollab.xyz'
JOIN users reporter  ON reporter.email  = 'loadtest' || ((ws_idx - 1) * 4 + (task_idx % 4) + 1) || '@letzcollab.xyz'
ON CONFLICT DO NOTHING;

SELECT setval('tasks_seq', (SELECT MAX(task_id) FROM tasks));


-- 7. task_comments (1 ~ 4,500,000)
-- 각 업무(2,250,000개)에 댓글 2개 생성
INSERT INTO task_comments (comment_id, task_id, author_id, content, parent_comment_id, created_at, updated_at)
SELECT
    ROW_NUMBER() OVER () AS comment_id,
    t.task_id,
    author.user_id,
    '부하 테스트 댓글 #' || comment_idx,
    NULL,
    NOW(),
    NOW()
FROM generate_series(1, 250000) AS ws_idx
CROSS JOIN generate_series(1, 3) AS proj_idx
CROSS JOIN generate_series(1, 3) AS task_idx
CROSS JOIN generate_series(1, 2) AS comment_idx
JOIN tasks t ON t.name = '업무-WS' || ws_idx || '-P' || proj_idx || '-T' || task_idx
JOIN users author ON author.email = 'loadtest' || ((ws_idx - 1) * 4 + ((comment_idx - 1) % 4) + 1) || '@letzcollab.xyz'
ON CONFLICT DO NOTHING;

SELECT setval('task_comments_seq', (SELECT MAX(comment_id) FROM task_comments));

-- 8. 알림 (1,000,000개)
-- 사용자 1명당 알림 1개씩
INSERT INTO notifications (
    notification_id, recipient_id, type, reference_type, reference_id, parent_reference_id, message, is_read, created_at
)
SELECT
    i AS notification_id,
    u.user_id,
    (ARRAY[
         'TASK_ASSIGNED',
         'TASK_STATUS_CHANGED',
         'TASK_REASSIGNED',
         'TASK_DUE_SOON',
         'TASK_OVERDUE',
         'COMMENT_ADDED',
         'COMMENT_REPLY_ADDED',
         'PROJECT_MEMBER_ADDED',
         'PROJECT_ROLE_CHANGED',
         'PROJECT_MEMBER_REMOVED'
    ])[(i % 10) + 1],
    (ARRAY['TASK', 'PROJECT'])[(i % 2) + 1],
    gen_random_uuid(),
    NULL,
    (ARRAY[
        '업무가 할당되었습니다',
        '업무 상태가 변경되었습니다',
        '업무 담당자가 변경되었습니다',
        '업무 마감일이 임박합니다',
        '업무가 마감일을 초과했습니다',
        '댓글이 달렸습니다',
        '대댓글이 달렸습니다',
        '프로젝트에 초대되었습니다',
        '프로젝트에서 권한이 변경되었습니다',
        '프로젝트에서 강퇴되었습니다'
    ])[(i % 10) + 1],
    (i % 2 = 0),                            -- 짝수: 읽음, 홀수: 안 읽음
    NOW() - (i % 30 || ' days')::interval   -- 최근 30일 내 분산
FROM generate_series(1, 1000000) AS i
JOIN users u ON u.user_id = i
ON CONFLICT DO NOTHING;

SELECT setval('notifications_seq', (SELECT MAX(notification_id) FROM notifications));