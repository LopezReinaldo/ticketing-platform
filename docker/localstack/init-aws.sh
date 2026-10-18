#!/bin/bash
# Provisions local DynamoDB tables and SQS queues when LocalStack is ready.
# Mirrors the resources that Terraform would create in AWS.
set -euo pipefail

REGION="${AWS_DEFAULT_REGION:-us-east-1}"

echo ">> Creating DynamoDB tables"

awslocal dynamodb create-table \
  --table-name events \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region "$REGION" >/dev/null

awslocal dynamodb create-table \
  --table-name tickets \
  --attribute-definitions \
      AttributeName=id,AttributeType=S \
      AttributeName=eventId,AttributeType=S \
      AttributeName=status,AttributeType=S \
      AttributeName=orderId,AttributeType=S \
      AttributeName=reservedUntil,AttributeType=N \
  --key-schema AttributeName=id,KeyType=HASH \
  --global-secondary-indexes \
      'IndexName=eventId-status-index,KeySchema=[{AttributeName=eventId,KeyType=HASH},{AttributeName=status,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
      'IndexName=orderId-index,KeySchema=[{AttributeName=orderId,KeyType=HASH}],Projection={ProjectionType=ALL}' \
      'IndexName=status-reservedUntil-index,KeySchema=[{AttributeName=status,KeyType=HASH},{AttributeName=reservedUntil,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
  --billing-mode PAY_PER_REQUEST \
  --region "$REGION" >/dev/null

awslocal dynamodb create-table \
  --table-name orders \
  --attribute-definitions \
      AttributeName=id,AttributeType=S \
      AttributeName=idempotencyKey,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --global-secondary-indexes \
      'IndexName=idempotencyKey-index,KeySchema=[{AttributeName=idempotencyKey,KeyType=HASH}],Projection={ProjectionType=ALL}' \
  --billing-mode PAY_PER_REQUEST \
  --region "$REGION" >/dev/null

echo ">> Creating SQS queues (main + dead-letter)"

DLQ_URL=$(awslocal sqs create-queue --queue-name purchase-requests-dlq --region "$REGION" --query QueueUrl --output text)
DLQ_ARN=$(awslocal sqs get-queue-attributes --queue-url "$DLQ_URL" --attribute-names QueueArn --query Attributes.QueueArn --output text)

awslocal sqs create-queue \
  --queue-name purchase-requests \
  --region "$REGION" \
  --attributes "{
    \"VisibilityTimeout\": \"30\",
    \"ReceiveMessageWaitTimeSeconds\": \"20\",
    \"RedrivePolicy\": \"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"
  }" >/dev/null

echo ">> LocalStack resources ready"
