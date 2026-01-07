#!/bin/bash
set -e

echo "=========================================="
echo "RabbitMQ Classic Queue Mirroring 설정"
echo "=========================================="

# RabbitMQ가 완전히 시작될 때까지 대기
echo "RabbitMQ 클러스터 준비 대기 중..."
sleep 10

# Policy 설정: 모든 큐를 모든 노드에 미러링
echo "Mirroring Policy 설정 중..."

rabbitmqctl -n rabbit@rabbitmq-1 set_policy ha-all \
    ".*" \
    '{"ha-mode":"all","ha-sync-mode":"automatic","ha-promote-on-failure":"always","ha-promote-on-shutdown":"always"}' \
    --priority 1 \
    --apply-to queues

echo ""
echo "✅ Mirroring Policy 설정 완료!"
echo ""
echo "Policy 설정 내용:"
echo "  - ha-mode: all (모든 노드에 미러링)"
echo "  - ha-sync-mode: automatic (자동 동기화)"
echo "  - ha-promote-on-failure: always (장애 시 자동 승격)"
echo "  - ha-promote-on-shutdown: always (종료 시 자동 승격)"
echo ""

# Policy 확인
echo "현재 Policy 목록:"
rabbitmqctl -n rabbit@rabbitmq-1 list_policies

echo ""
echo "클러스터 상태:"
rabbitmqctl -n rabbit@rabbitmq-1 cluster_status

echo ""
echo "=========================================="
echo "설정 완료! 모든 큐가 자동으로 미러링됩니다."
echo "=========================================="
