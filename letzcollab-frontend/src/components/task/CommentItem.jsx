import { useState } from 'react';
import { Avatar, Button, Flex, Input, message, Modal, Typography } from 'antd';
import { DeleteOutlined, EditOutlined, ExclamationCircleOutlined, SendOutlined, UserOutlined } from '@ant-design/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../api/axios.js';
import { timeAgo } from '../../utils/dateUtils.js';

const { Text } = Typography;
const { TextArea } = Input;
const { confirm } = Modal;

export default function CommentItem({ comment, currentUserName, projectPublicId, taskPublicId, isReply = false }) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);
  const [replyOpen, setReplyOpen] = useState(false);
  const [replyContent, setReplyContent] = useState('');

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['comments', taskPublicId] });

  const updateMutation = useMutation({
    mutationFn: () =>
      api.patch(`/projects/${projectPublicId}/tasks/${taskPublicId}/comments/${comment.commentId}`, {
        newContent: editContent,
      }),
    onSuccess: () => {
      message.success('댓글이 수정되었습니다.');
      setEditing(false);
      invalidate();
    },
    onError: (e) => message.error(e.response?.data?.message || '수정에 실패했습니다.'),
  });

  const deleteMutation = useMutation({
    mutationFn: () =>
      api.delete(`/projects/${projectPublicId}/tasks/${taskPublicId}/comments/${comment.commentId}`),
    onSuccess: () => {
      message.success('댓글이 삭제되었습니다.');
      invalidate();
    },
    onError: (e) => message.error(e.response?.data?.message || '삭제에 실패했습니다.'),
  });

  const replyMutation = useMutation({
    mutationFn: () =>
      api.post(`/projects/${projectPublicId}/tasks/${taskPublicId}/comments`, {
        content: replyContent,
        parentCommentId: comment.commentId,
      }),
    onSuccess: () => {
      message.success('답글이 작성되었습니다.');
      setReplyContent('');
      setReplyOpen(false);
      invalidate();
    },
    onError: (e) => message.error(e.response?.data?.message || '답글 작성에 실패했습니다.'),
  });

  const handleDelete = () =>
    confirm({
      title: '댓글을 삭제하시겠습니까?',
      icon: <ExclamationCircleOutlined/>,
      content: isReply ? '답글이 삭제됩니다.' : '댓글과 모든 답글이 삭제됩니다.',
      okText: '삭제',
      okType: 'danger',
      cancelText: '취소',
      onOk: () => deleteMutation.mutate(),
    });

  const isMyComment = comment.authorName === currentUserName;
  // updatedAt이 createdAt과 다를 때만 수정됨 표시
  const isEdited = comment.updatedAt && comment.updatedAt !== comment.createdAt;
  const timeStr = timeAgo(comment.updatedAt ?? comment.createdAt);

  return (
    <div style={{ marginLeft: isReply ? 32 : 0 }}>
      <Flex gap={10} align="flex-start">
        <Avatar
          size={isReply ? 28 : 32}
          icon={<UserOutlined/>}
          style={{ flexShrink: 0, marginTop: 2 }}
        />
        <Flex vertical gap={4} style={{ flex: 1, minWidth: 0 }}>
          {/* 작성자 + 시간 */}
          <Flex align="center" gap={8}>
            <Text strong style={{ fontSize: 13 }}>{comment.authorName}</Text>
            <Text type="secondary" style={{ fontSize: 11 }}>
              {timeStr}{isEdited ? ' (수정됨)' : ''}
            </Text>
          </Flex>

          {/* 본문 or 수정 폼 */}
          {editing ? (
            <Flex vertical gap={8}>
              <TextArea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                autoSize={{ minRows: 2, maxRows: 6 }}
              />
              <Flex gap={8} justify="flex-end">
                <Button size="small" onClick={() => {
                  setEditing(false);
                  setEditContent(comment.content);
                }}>
                  취소
                </Button>
                <Button
                  size="small"
                  type="primary"
                  loading={updateMutation.isPending}
                  disabled={!editContent.trim()}
                  onClick={() => updateMutation.mutate()}
                >
                  저장
                </Button>
              </Flex>
            </Flex>
          ) : (
            <Text style={{ fontSize: 13, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
              {comment.content}
            </Text>
          )}

          {/* 액션 버튼 */}
          {!editing && (
            <Flex gap={8} align="center">
              {!isReply && (
                <Button
                  type="text"
                  size="small"
                  style={{ padding: '0 4px', fontSize: 12, color: '#8c8c8c', height: 'auto' }}
                  onClick={() => setReplyOpen(!replyOpen)}
                >
                  답글
                </Button>
              )}
              {isMyComment && (
                <>
                  <Button
                    type="text" size="small" icon={<EditOutlined/>}
                    style={{ padding: '0 4px', fontSize: 12, color: '#8c8c8c', height: 'auto' }}
                    onClick={() => setEditing(true)}
                  />
                  <Button
                    type="text" size="small" icon={<DeleteOutlined/>}
                    style={{ padding: '0 4px', fontSize: 12, color: '#ff4d4f', height: 'auto' }}
                    loading={deleteMutation.isPending}
                    onClick={handleDelete}
                  />
                </>
              )}
            </Flex>
          )}

          {/* 답글 입력 */}
          {replyOpen && (
            <Flex gap={8} align="flex-end" style={{ marginTop: 4 }}>
              <TextArea
                value={replyContent}
                onChange={(e) => setReplyContent(e.target.value)}
                placeholder="답글을 입력하세요..."
                autoSize={{ minRows: 1, maxRows: 4 }}
                style={{ flex: 1 }}
              />
              <Button
                type="primary"
                icon={<SendOutlined/>}
                loading={replyMutation.isPending}
                disabled={!replyContent.trim()}
                onClick={() => replyMutation.mutate()}
              />
            </Flex>
          )}
        </Flex>
      </Flex>

      {/* 대댓글 재귀 렌더 */}
      {!isReply && comment.replies?.length > 0 && (
        <Flex vertical gap={12} style={{ marginTop: 12 }}>
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.commentId}
              comment={reply}
              currentUserName={currentUserName}
              projectPublicId={projectPublicId}
              taskPublicId={taskPublicId}
              isReply
            />
          ))}
        </Flex>
      )}
    </div>
  );
}