#!/bin/bash
set -e

# RabbitMQ 클러스터 조인 스크립트
# Node 2, 3이 Node 1에 조인하도록 설정

echo "=========================================="
echo "RabbitMQ Cluster Node Starting..."
echo "Node: ${RABBITMQ_NODENAME}"
echo "=========================================="

# RabbitMQ 기본 entrypoint 실행
docker-entrypoint.sh rabbitmq-server &

# RabbitMQ가 시작될 때까지 대기
echo "Waiting for RabbitMQ to start..."
sleep 15

# 클러스터에 조인
echo "Joining cluster..."
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl join_cluster rabbit@rabbitmq-1
rabbitmqctl start_app

echo "Cluster join completed!"
echo "=========================================="

# 클러스터 상태 확인
sleep 5
echo "Cluster status:"
rabbitmqctl cluster_status

# 프로세스가 종료되지 않도록 대기
wait
