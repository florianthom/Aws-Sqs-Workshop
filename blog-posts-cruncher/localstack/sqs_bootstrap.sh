#!/usr/bin/env bash

LOCALSTACK_HOST=localhost
AWS_REGION=eu-central-1

create_queue() {
    local QUEUE_NAME_TO_CREATE=$1
    awslocal --endpoint-url=http://${LOCALSTACK_HOST}:4566 sqs create-queue --queue-name ${QUEUE_NAME_TO_CREATE} --region ${AWS_REGION} --attributes '{"MessageRetentionPeriod":"3600"}'
}

MessageRetentionPeriod

create_queue "inputqueue"
create_queue "resultqueue"