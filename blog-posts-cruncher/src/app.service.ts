import { Injectable, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import { SQSClient, ReceiveMessageCommand, DeleteMessageCommand, SendMessageCommand } from '@aws-sdk/client-sqs';
import { ConfigService } from '@nestjs/config';

// $: AWS_ACCESS_KEY_ID="test" AWS_SECRET_ACCESS_KEY="test" AWS_DEFAULT_REGION="eu-central-1" aws --endpoint-url=http://localhost:4566 sts get-caller-identity
// $: AWS_ACCESS_KEY_ID="test" AWS_SECRET_ACCESS_KEY="test" AWS_DEFAULT_REGION="eu-central-1" aws --endpoint-url=http://localhost:4566 sqs purge-queue --queue-url http://localhost:4571/000000000000/inputqueue
// $: AWS_ACCESS_KEY_ID="test" AWS_SECRET_ACCESS_KEY="test" AWS_DEFAULT_REGION="eu-central-1" aws --endpoint-url=http://localhost:4566 sqs send-message --queue-url http://localhost:4571/000000000000/inputqueue --message-body "Hello SQS!"
// $: AWS_ACCESS_KEY_ID="test" AWS_SECRET_ACCESS_KEY="test" AWS_DEFAULT_REGION="eu-central-1" aws --endpoint-url=http://localhost:4566 sqs send-message --queue-url http://localhost:4571/000000000000/inputqueue --message-body "{\"title\": \"blogpost title\", \"content\": \"blog post content\"}"

interface BlogPostJobRequestDto {
  title: string;
  content: string;
}

interface BlogPostJobResultDto {
  status: string;
  content: string;
}

@Injectable()
export class AppService implements OnModuleInit, OnModuleDestroy{
  private sqsClient: SQSClient;
  private queueUrl: string;
  private resultQueueUrl: string;
  private isRunning = true;

  constructor(private readonly configService: ConfigService) {}

  async onModuleInit() {
    this.sqsClient = new SQSClient({
      region: this.configService.get<string>('AWS_REGION'),
      endpoint: this.configService.getOrThrow<string>('AWS_SQS_ENDPOINT'),
      credentials: {
        accessKeyId: this.configService.getOrThrow<string>('AWS_ACCESS_KEY_ID'),
        secretAccessKey: this.configService.getOrThrow<string>('AWS_SECRET_ACCESS_KEY')
      }
    });

    this.queueUrl = this.configService.getOrThrow<string>('AWS_SQS_QUEUE_URL');
    this.resultQueueUrl = this.configService.getOrThrow<string>('AWS_SQS_RESULT_QUEUE_URL');

    console.log('blog posts cruncher ready and listening');

    while (this.isRunning) {
        const response = await this.sqsClient.send(new ReceiveMessageCommand({
          QueueUrl: this.queueUrl,
          MaxNumberOfMessages: 1,
          WaitTimeSeconds: 20,
          VisibilityTimeout: 300 // please keep this > processing time
        }));

        if (response.Messages === undefined || response.Messages.length === 0) continue;
        
        const message = response.Messages![0]

        console.log('Received message:', message.Body);

        const result = await this.processMessage(JSON.parse(message.Body!) as BlogPostJobRequestDto);

        const jobResultDto = {status: "FINISHED", content: result} as BlogPostJobResultDto
    
        await this.sqsClient.send(new SendMessageCommand({
          QueueUrl: this.resultQueueUrl,
          MessageBody: JSON.stringify(jobResultDto),
        }));

        console.log('Sent result message:', result);

        await this.sqsClient.send(new DeleteMessageCommand({
          QueueUrl: this.queueUrl,
          ReceiptHandle: message.ReceiptHandle,
        }));

        console.log('Deleted message from queue');
    }
  }

  async processMessage(messageBody: BlogPostJobRequestDto): Promise<string> {
    console.log(`blog post title: ${messageBody.title} and content: ${messageBody.content}`);
    return "finished blog post with title: " + messageBody.title;
  }

  onModuleDestroy() {
    this.isRunning = false;
    console.log('SQS Worker stopped.');
  }
}
