# 빌드 단계
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 실행 단계 (nginx 실행 + 빌드된 리액트 앱 서빙)
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80 443
# nginx 실행 상태를 foreground로 바꿔야 컨테이너가 바로 종료 안 함
CMD ["nginx", "-g", "daemon off;"]