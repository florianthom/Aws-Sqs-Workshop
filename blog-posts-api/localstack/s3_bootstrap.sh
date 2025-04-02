#!/usr/bin/env bash

LOCALSTACK_HOST=localhost
AWS_REGION=eu-central-1

awslocal --endpoint-url=http://${LOCALSTACK_HOST}:4566 --region ${AWS_REGION} s3 mb s3://blog-posts-api-bucket