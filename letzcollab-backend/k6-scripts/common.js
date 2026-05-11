import {SharedArray} from 'k6/data'

export const BASE_URL = 'http://localhost:8080/api';

export const tokens = new SharedArray('user tokens',
  () => open('../tokens.txt').split('\n').filter(t => t.trim().length > 0));
export const workspaceIds = new SharedArray('workspace ids',
  () => open('../workspacePublicIds.txt').split('\n').filter(t => t.trim().length > 0));

export function commonSetup() {
  console.log(`로드 완료: 토큰 ${tokens.length}개 / 워크스페이스ID ${workspaceIds.length}개`);
  // 데이터 개수가 맞지 않으면 경고 출력
  if (tokens.length !== workspaceIds.length) {
    console.warn('경고: 토큰과 워크스페이스 ID의 개수가 일치하지 않습니다!');
  }
}