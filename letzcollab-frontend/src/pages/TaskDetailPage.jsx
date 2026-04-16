import { useState } from 'react';
import { Avatar, Badge, Button, Card, Divider, Flex, Input, message, Modal, Skeleton, Tag, Typography, } from 'antd';
import {
  ArrowLeftOutlined,
  BranchesOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  FlagOutlined,
  MessageOutlined,
  PlusOutlined,
  SendOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../api/axios.js';
import { useCurrentUser } from '../hooks/useCurrentUser.js';
import { useMyProjectRole } from '../hooks/useMyProjectRole.js';
import { formatDateTime, formatDueDate } from '../utils/dateUtils.js';
import { PRIORITY_CONFIG, STATUS_CONFIG } from '../constants/taskStatus.js';
import SubTaskCard from '../components/task/SubTaskCard.jsx';
import CommentItem from '../components/task/CommentItem.jsx';
import EditTaskModal from '../components/task/EditTaskModal.jsx';
import CreateSubTaskModal from '../components/task/CreateSubTaskModal.jsx';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;
const { confirm } = Modal;

const LAST_WORKSPACE_KEY = 'lastWorkspaceId';

export default function TaskDetailPage() {
  const { projectPublicId, taskPublicId } = useParams();
  const nav = useNavigate();
  const queryClient = useQueryClient();

  const workspacePublicId = localStorage.getItem(LAST_WORKSPACE_KEY);

  const [commentContent, setCommentContent] = useState('');
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [subTaskModalOpen, setSubTaskModalOpen] = useState(false);

  const { data: me } = useCurrentUser();

  // 업무 상세 조회
  const { data: task, isLoading: taskLoading } = useQuery({
    queryKey: ['task', taskPublicId],
    queryFn: async () => {
      const res = await api.get(`/projects/${projectPublicId}/tasks/${taskPublicId}`);
      return res.data.data;
    },
    refetchInterval: 1000 * 60,
    refetchIntervalInBackground: false,
  });

  // 댓글 목록 조회
  const { data: comments = [], isLoading: commentsLoading } = useQuery({
    queryKey: ['comments', taskPublicId],
    queryFn: async () => {
      const res = await api.get(`/projects/${projectPublicId}/tasks/${taskPublicId}/comments`);
      return res.data.data;
    },
    refetchInterval: 1000 * 60,
    refetchIntervalInBackground: false,
  });

  // 내 프로젝트 권한 조회
  const { data: myMember } = useMyProjectRole(projectPublicId);

  // 권한 판별
  const myPublicId = me?.publicId;
  const myRole = myMember?.role;

  const isReporter = !!task && task.reporterPublicId === myPublicId;
  const isAssignee = !!task && task.assigneePublicId === myPublicId;
  const isAdmin = myRole === 'ADMIN';
  const isMember = myRole === 'MEMBER';

  // 업무 수정: ADMIN이거나 reporter
  const canEditAll = isAdmin || isReporter;
  // 상태만 수정: 담당자(canEditAll 포함)
  const canEdit = canEditAll || isAssignee;
  // 업무 삭제: ADMIN이거나 reporter
  const canDelete = isAdmin || isReporter;
  // 하위 업무 생성: ADMIN이거나 MEMBER 중 reporter 또는 assignee
  const canCreateSubTask = isAdmin || (isMember && (isReporter || isAssignee));

  // 댓글 작성
  const commentMutation = useMutation({
    mutationFn: () =>
      api.post(`/projects/${projectPublicId}/tasks/${taskPublicId}/comments`, {
        content: commentContent,
        parentCommentId: null,
      }),
    onSuccess: () => {
      message.success('댓글이 작성되었습니다.');
      setCommentContent('');
      queryClient.invalidateQueries({ queryKey: ['comments', taskPublicId] });
    },
    onError: (e) => message.error(e.response?.data?.message || '댓글 작성에 실패했습니다.'),
  });

  // 업무 삭제
  const deleteMutation = useMutation({
    mutationFn: () => api.delete(`/projects/${projectPublicId}/tasks/${taskPublicId}`),
    onSuccess: () => {
      message.success('업무가 삭제되었습니다.');
      nav(-1);
    },
    onError: (e) => message.error(e.response?.data?.message || '삭제에 실패했습니다.'),
  });

  const handleDelete = () =>
    confirm({
      title: '업무를 삭제하시겠습니까?',
      icon: <ExclamationCircleOutlined/>,
      content: '하위 업무도 함께 삭제됩니다. 이 작업은 되돌릴 수 없습니다.',
      okText: '삭제', okType: 'danger', cancelText: '취소',
      onOk: () => deleteMutation.mutate(),
    });

  const status = task ? STATUS_CONFIG[task.status] ?? { color: 'default', label: task.status } : null;
  const priority = task ? PRIORITY_CONFIG[task.priority] ?? { color: 'default', label: task.priority } : null;
  const due = task ? formatDueDate(task.dueDate) : null;

  // 로딩 / 에러
  if (taskLoading) {
    return (
      <div style={{ padding: '28px 32px', maxWidth: 900, margin: '0 auto' }}>
        <Skeleton active paragraph={{ rows: 8 }}/>
      </div>
    );
  }

  if (!task) {
    return (
      <Flex justify="center" align="center" style={{ minHeight: 400 }}>
        <Text type="secondary">업무를 찾을 수 없습니다.</Text>
      </Flex>
    );
  }

  return (
    <div style={{ padding: '24px 32px', maxWidth: 900, margin: '0 auto' }}>

      <Button
        type="text" icon={<ArrowLeftOutlined/>}
        onClick={() => nav(-1)}
        style={{ marginBottom: 16, padding: '0 4px', color: '#595959' }}
      >
        돌아가기
      </Button>

      {/* 헤더: 제목 + 태그 + 액션 버튼 */}
      <Flex justify="space-between" align="flex-start" gap={16} style={{ marginBottom: 20 }}>
        <Flex vertical gap={8} style={{ minWidth: 0 }}>
          {task.parentTaskPublicId && (
            <Flex align="center" gap={4}>
              <BranchesOutlined style={{ fontSize: 12, color: '#8c8c8c' }}/>
              <Text
                type="secondary"
                style={{ fontSize: 12, cursor: 'pointer' }}
                onClick={() => nav(`/projects/${projectPublicId}/tasks/${task.parentTaskPublicId}`)}
              >
                상위 업무로 이동
              </Text>
            </Flex>
          )}
          <Title level={4} style={{ margin: 0 }}>{task.name}</Title>
          <Flex gap={8} wrap="wrap">
            <Tag color={priority.color} icon={<FlagOutlined/>}>{priority.label}</Tag>
            <Tag color={status.color}>{status.label}</Tag>
            {due && (
              <Tag color={due.danger ? 'red' : 'default'} icon={<ClockCircleOutlined/>}>
                {due.text}
              </Tag>
            )}
          </Flex>
        </Flex>

        <Flex gap={8} style={{ flexShrink: 0 }}>
          {canEdit && (
            <Button icon={<EditOutlined/>} onClick={() => setEditModalOpen(true)}>수정</Button>
          )}
          {canDelete && (
            <Button danger icon={<DeleteOutlined/>} loading={deleteMutation.isPending} onClick={handleDelete}>
              삭제
            </Button>
          )}
        </Flex>
      </Flex>

      {/* 본문 2단 레이아웃 */}
      <Flex gap={24} align="flex-start" wrap="wrap">

        {/* 왼쪽: 설명  하위 업무  댓글 */}
        <Flex vertical gap={16} style={{ flex: '1 1 520px', minWidth: 0 }}>

          {/* 설명 */}
          <Card size="small" title="설명">
            {task.description
              ? <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{task.description}</Paragraph>
              : <Text type="secondary" style={{ fontSize: 13 }}>설명이 없습니다.</Text>
            }
          </Card>

          {/* 하위 업무 */}
          {(task.subTasks?.length > 0 || canCreateSubTask) && (
            <Card
              size="small"
              title={
                <Flex align="center" gap={8}>
                  <BranchesOutlined/>
                  <span>하위 업무</span>
                  {task.subTasks?.length > 0 && (
                    <Badge count={task.subTasks.length} color="#8c8c8c"/>
                  )}
                </Flex>
              }
              extra={
                canCreateSubTask && (
                  <Button
                    type="text"
                    size="small"
                    icon={<PlusOutlined/>}
                    onClick={() => setSubTaskModalOpen(true)}
                  >
                    추가
                  </Button>
                )
              }
            >
              {task.subTasks?.length > 0 ? (
                <Flex vertical gap={6}>
                  {task.subTasks.map((sub) => (
                    <SubTaskCard
                      key={sub.publicId}
                      subTask={sub}
                      projectPublicId={projectPublicId}
                    />
                  ))}
                </Flex>
              ) : (
                <Text type="secondary" style={{ fontSize: 13 }}>하위 업무가 없습니다.</Text>
              )}
            </Card>
          )}

          {/* 댓글 */}
          <Card
            size="small"
            title={
              <Flex align="center" gap={8}>
                <MessageOutlined/>
                <span>댓글</span>
                <Badge count={comments.length} color="#8c8c8c"/>
              </Flex>
            }
          >
            {/* 댓글 작성 폼 */}
            <Flex gap={10} align="flex-start" style={{ marginBottom: 16 }}>
              <Avatar size={32} icon={<UserOutlined/>} style={{ flexShrink: 0, marginTop: 2 }}/>
              <Flex vertical gap={8} style={{ flex: 1 }}>
                <TextArea
                  value={commentContent}
                  onChange={(e) => setCommentContent(e.target.value)}
                  placeholder="댓글을 입력하세요..."
                  autoSize={{ minRows: 2, maxRows: 6 }}
                />
                <Flex justify="flex-end">
                  <Button
                    type="primary" icon={<SendOutlined/>}
                    loading={commentMutation.isPending}
                    disabled={!commentContent.trim()}
                    onClick={() => commentMutation.mutate()}
                  >
                    작성
                  </Button>
                </Flex>
              </Flex>
            </Flex>

            <Divider style={{ margin: '8px 0 16px' }}/>

            {/* 댓글 목록 */}
            {commentsLoading ? (
              <Skeleton active paragraph={{ rows: 3 }}/>
            ) : comments.length === 0 ? (
              <Flex justify="center" style={{ padding: '24px 0' }}>
                <Text type="secondary" style={{ fontSize: 13 }}>아직 댓글이 없습니다.</Text>
              </Flex>
            ) : (
              <Flex vertical gap={16}>
                {comments.map((c) => (
                  <CommentItem
                    key={c.commentId}
                    comment={c}
                    currentUserName={me?.name}
                    projectPublicId={projectPublicId}
                    taskPublicId={taskPublicId}
                  />
                ))}
              </Flex>
            )}
          </Card>
        </Flex>

        {/* 오른쪽: 메타 정보 */}
        <Card size="small" title="업무 정보" style={{ flex: '0 0 220px', minWidth: 200 }}>
          <Flex vertical gap={12}>
            <Flex vertical gap={4}>
              <Text type="secondary" style={{ fontSize: 11 }}>담당자</Text>
              <Flex align="center" gap={6}>
                <Avatar size={20} icon={<UserOutlined/>}/>
                <Text style={{ fontSize: 13 }}>{task.assigneeName}</Text>
              </Flex>
            </Flex>

            <Flex vertical gap={4}>
              <Text type="secondary" style={{ fontSize: 11 }}>보고자</Text>
              <Flex align="center" gap={6}>
                <Avatar size={20} icon={<UserOutlined/>}/>
                <Text style={{ fontSize: 13 }}>{task.reporterName}</Text>
              </Flex>
            </Flex>

            <Divider style={{ margin: '4px 0' }}/>

            <Flex vertical gap={4}>
              <Text type="secondary" style={{ fontSize: 11 }}>마감일</Text>
              <Flex align="center" gap={4}>
                <CalendarOutlined style={{ fontSize: 12, color: due?.danger ? '#ff4d4f' : '#8c8c8c' }}/>
                <Text style={{ fontSize: 13, color: due?.danger ? '#ff4d4f' : undefined }}>
                  {task.dueDate ?? '없음'}
                </Text>
              </Flex>
            </Flex>

            <Divider style={{ margin: '4px 0' }}/>

            <Flex vertical gap={4}>
              <Text type="secondary" style={{ fontSize: 11 }}>생성일</Text>
              <Text style={{ fontSize: 12 }} type="secondary">{formatDateTime(task.createdAt)}</Text>
            </Flex>
            <Flex vertical gap={4}>
              <Text type="secondary" style={{ fontSize: 11 }}>최종 수정</Text>
              <Text style={{ fontSize: 12 }} type="secondary">{formatDateTime(task.updatedAt)}</Text>
            </Flex>
          </Flex>
        </Card>
      </Flex>

      {/* 업무 수정 모달 */}
      {editModalOpen && (
        <EditTaskModal
          open={editModalOpen}
          onClose={() => setEditModalOpen(false)}
          task={task}
          projectPublicId={projectPublicId}
          canEditAll={canEditAll}
        />
      )}

      {/* 하위 업무 생성 모달 */}
      {subTaskModalOpen && (
        <CreateSubTaskModal
          open={subTaskModalOpen}
          onClose={() => setSubTaskModalOpen(false)}
          parentTaskPublicId={taskPublicId}
          projectPublicId={projectPublicId}
          workspacePublicId={workspacePublicId}
          myRole={myRole}
          myPublicId={myPublicId}
        />
      )}
    </div>
  );
}