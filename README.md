# Aws-Sqs-Workshop
Showcasing Job API Service - Job Worker Service - Collaboration using AWS SQS (communicate metadata) and AWS S3 (communicate large file results via presigned url) suited for deployments in k8s workspaces. This approach demonstrates a solution for a api-worker architecture where the api and worker cant be developed in the same lenguage but datamodel - drift should be avoided. The scenario here is that the api needs to create a large image of an blog-post with a task duration ~1min (for testing purposes ~1s)

## Prerequisites
To run the applications you need the following software
 - node v22+
 - pnpm v10+
 - java JDK distribution e.g. amazon coretto v21+
 - gradle v8+
 - docker & docker compose
 - aws cli
 - curl
 - tmux (just recommended)

## Getting Started
Since both application are seprated you start them individually:

1. Start the blog posts api

```
 $ cd blog-posts-cruncher
 $ docker compose up
 $ pnpm run start:dev
```

2. Start the blog posts worker (cruncher)

```
$ cd blog-posts-api
$ docker compose up
$ ./gradlew bootRun
```

## Hints
- localstacks SQS-Endpoint (localhost:4566) differs from the Queue-Endpoint (localhost:4571)
- both apps use localstack via docker compose - so the api exposes the localstack service on port 4567 (not 4566)
- SQS Queue `MessageRetentionPeriod`: 3600s
- SQS Queue Receive Message `VisibilityTimeout`: 300s

## Important commands
A selection of useful commands will be presented:
 - Call the api
```
$ curl localhost:8080/createsqsjob
```

 - Purge the sqs - queue via aws cli
```
$ AWS_ACCESS_KEY_ID="test" \
  AWS_SECRET_ACCESS_KEY="test" \
  AWS_DEFAULT_REGION="eu-central-1" \
  aws --endpoint-url=http://localhost:4566 sqs purge-queue \
      --queue-url http://localhost:4571/000000000000/inputqueue
```

 - Put item into the queue via aws cli
```
$ AWS_ACCESS_KEY_ID="test" \
  AWS_SECRET_ACCESS_KEY="test" \
  AWS_DEFAULT_REGION="eu-central-1" \
  aws --endpoint-url=http://localhost:4566 sqs send-message \
    --queue-url http://localhost:4571/000000000000/inputqueue \
    --message-body "{ \"jobId\": \"123\", \"title\": \"blogpost title\", \"content\": \"blog post content\", \"resultfileUploadUrl\": \"http://localhost:4567/blog-posts-api-bucket/item.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250402T183320Z&X-Amz-SignedHeaders=content-type%3Bhost&X-Amz-Credential=test%2F20250402%2Feu-central-1%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=a0570f50c649462049ce65a92293a9b28e7f4d58775336aff9e67bc4153cc332\"}"
```

- Check items in s3 (localstack)
```
$ AWS_ACCESS_KEY_ID="test" \
  AWS_SECRET_ACCESS_KEY="test" \
  AWS_DEFAULT_REGION="eu-central-1" \
  aws --endpoint-url=http://localhost:4567 s3 ls s3://blog-posts-api-bucket/
```

- Download from s3 (localstack)
```
$ AWS_ACCESS_KEY_ID="test" \
  AWS_SECRET_ACCESS_KEY="test" \
  AWS_DEFAULT_REGION="eu-central-1" \
  aws --endpoint-url=http://localhost:4567 s3 cp s3://blog-posts-api-bucket/item.jpg ./item.jpg
```

## Build with
 - spring boot v3
 - nestjs v11
 - localstack
 - aws s3
 - aws sqs