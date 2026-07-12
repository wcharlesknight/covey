# Weekly Job Infrastructure Deployment Guide

This guide covers deploying the EventBridge rules and CloudWatch observability for the weekly job pipeline.

## Prerequisites

- AWS CLI v2 installed and configured
- Appropriate IAM permissions (CloudFormation, Lambda, EventBridge, CloudWatch, IAM)
- Lambda functions already deployed:
  - `covey-weekly-spot-dev` (or `prod`)
  - `covey-notification-delivery-dev` (or `prod`)

## Architecture Overview

### Scheduled Triggers

```
Every Thursday 9 PM EST
    ↓
EventBridge Rule → covey-weekly-spot-dev Lambda
    ↓
Selects venues, creates WeeklySpot + Invite docs
    ↓
Queues notifications in scheduledNotifications Firestore collection

Every Friday 9 AM EST
    ↓
EventBridge Rule → covey-notification-delivery-dev Lambda
    ↓
Drains queue, sends FCM + SES notifications
    ↓
Marks tasks SENT/FAILED, moves hard failures to DLQ
```

### Observability

- **Logs:** CloudWatch Log Groups with 30-day retention
- **Metrics:** Lambda invocations, errors, duration, throttles
- **Alarms:** Alert on errors and throttles
- **Dashboard:** Unified view of job health and metrics

## Deployment Steps

### Step 1: Get Lambda Function ARNs

```bash
# Get the ARN of your Lambda functions
aws lambda get-function \
  --function-name covey-weekly-spot-dev \
  --region us-west-2 \
  --query 'Configuration.FunctionArn' \
  --output text

# Example output:
# arn:aws:lambda:us-west-2:123456789012:function:covey-weekly-spot-dev

# Do the same for notification delivery
aws lambda get-function \
  --function-name covey-notification-delivery-dev \
  --region us-west-2 \
  --query 'Configuration.FunctionArn' \
  --output text
```

### Step 2: Deploy CloudFormation Stack

```bash
# Deploy to dev environment
aws cloudformation deploy \
  --template-file infrastructure/eventbridge-weekly-job.yaml \
  --stack-name covey-weekly-job-dev \
  --parameter-overrides \
    Environment=dev \
    LambdaFunctionArn=arn:aws:lambda:us-west-2:123456789012:function:covey-weekly-spot-dev \
    NotificationLambdaFunctionArn=arn:aws:lambda:us-west-2:123456789012:function:covey-notification-delivery-dev \
  --region us-west-2 \
  --capabilities CAPABILITY_NAMED_IAM
```

### Step 3: Verify Deployment

```bash
# Check EventBridge rules are enabled
aws events list-rules \
  --name-prefix covey-weekly \
  --region us-west-2

# Check log groups exist
aws logs describe-log-groups \
  --log-group-name-prefix '/aws/lambda/covey-' \
  --region us-west-2

# Check alarms are created
aws cloudwatch describe-alarms \
  --alarm-name-prefix 'covey-weekly' \
  --region us-west-2
```

## Configuration

### Adjust Schedule Times

Edit `eventbridge-weekly-job.yaml` and modify the cron expressions:

```yaml
# Current: Thursday 9 PM EST (UTC-5) or EDT (UTC-4)
# Use: cron(0 21 ? * THU *)

# To change to Thursday 11 PM EST:
# Use: cron(0 23 ? * THU *)

# Friday 9 AM EST:
# Use: cron(0 9 ? * FRI *)

# Note: AWS EventBridge uses UTC time. Adjust accordingly:
# EST (UTC-5): 21:00 UTC = 4 PM EST (add 5 hours)
# EDT (UTC-4): 21:00 UTC = 5 PM EDT (add 4 hours)
```

**Recommended Times (UTC):**
- Selection: `cron(0 2 ? * FRI *)` = Thursday 9 PM EST/EDT
- Delivery: `cron(0 13 ? * FRI *)` = Friday 9 AM EST/EDT

### Configure Alarm Notifications

Add SNS topic for alarm notifications:

```bash
# Create SNS topic
aws sns create-topic \
  --name covey-alerts \
  --region us-west-2

# Add to template Alarms section:
AlarmActions:
  - arn:aws:sns:us-west-2:123456789012:covey-alerts

# Subscribe your email
aws sns subscribe \
  --topic-arn arn:aws:sns:us-west-2:123456789012:covey-alerts \
  --protocol email \
  --notification-endpoint your-email@example.com
```

### Adjust Retry Policy

Modify EventBridge Target RetryPolicy:

```yaml
RetryPolicy:
  MaximumEventAge: 3600      # Max 1 hour old
  MaximumRetryAttempts: 2    # Retry up to 2 times
```

**For more aggressive retries:**
```yaml
RetryPolicy:
  MaximumEventAge: 86400     # Max 24 hours old
  MaximumRetryAttempts: 5    # Retry up to 5 times
```

## Monitoring

### View CloudWatch Dashboard

```bash
# Get dashboard URL
aws cloudformation describe-stacks \
  --stack-name covey-weekly-job-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`DashboardURL`].OutputValue' \
  --output text \
  --region us-west-2

# Or access directly:
# https://console.aws.amazon.com/cloudwatch/home?region=us-west-2#dashboards:name=covey-weekly-job-dev
```

### Query Logs

**Find all weekly job executions:**
```bash
aws logs filter-log-events \
  --log-group-name '/aws/lambda/covey-weekly-spot-dev' \
  --filter-pattern 'Weekly job' \
  --region us-west-2
```

**Find all errors:**
```bash
aws logs filter-log-events \
  --log-group-name '/aws/lambda/covey-weekly-spot-dev' \
  --filter-pattern 'ERROR' \
  --region us-west-2
```

**CloudWatch Insights query:**
```
fields @timestamp, @message, @duration
| filter @message like /Weekly job/
| stats count() as invocations, avg(@duration) as avg_duration by bin(1h)
```

### Manual Test Invocation

Test the selection job:
```bash
aws lambda invoke \
  --function-name covey-weekly-spot-dev \
  --invocation-type RequestResponse \
  --payload '{}' \
  --region us-west-2 \
  response.json

cat response.json
```

Test the delivery job:
```bash
aws lambda invoke \
  --function-name covey-notification-delivery-dev \
  --invocation-type RequestResponse \
  --payload '{}' \
  --region us-west-2 \
  response.json

cat response.json
```

## Troubleshooting

### Rule Not Triggering

**Check if rule is enabled:**
```bash
aws events describe-rule \
  --name covey-weekly-selection-dev \
  --region us-west-2
```

**Enable rule if disabled:**
```bash
aws events enable-rule \
  --name covey-weekly-selection-dev \
  --region us-west-2
```

### Lambda Invocation Fails

**Check EventBridge role permissions:**
```bash
aws iam get-role-policy \
  --role-name WeeklySelectionEventBridgeRole \
  --policy-name InvokeLambda
```

**Check Lambda resource policy:**
```bash
aws lambda get-policy \
  --function-name covey-weekly-spot-dev \
  --region us-west-2
```

### Alarms Not Triggering

**Check alarm state:**
```bash
aws cloudwatch describe-alarms \
  --alarm-names covey-weekly-job-errors-dev \
  --region us-west-2
```

**Test alarm:**
```bash
aws cloudwatch set-alarm-state \
  --alarm-name covey-weekly-job-errors-dev \
  --state-value ALARM \
  --state-reason "Testing alarm" \
  --region us-west-2
```

## Updating the Stack

To update EventBridge rules or alarms:

```bash
# Update stack with new parameters
aws cloudformation update-stack \
  --stack-name covey-weekly-job-dev \
  --template-body file://infrastructure/eventbridge-weekly-job.yaml \
  --parameters \
    ParameterKey=Environment,ParameterValue=dev \
    ParameterKey=LambdaFunctionArn,ParameterValue=arn:aws:lambda:us-west-2:123456789012:function:covey-weekly-spot-dev \
    ParameterKey=NotificationLambdaFunctionArn,ParameterValue=arn:aws:lambda:us-west-2:123456789012:function:covey-notification-delivery-dev \
  --region us-west-2 \
  --capabilities CAPABILITY_NAMED_IAM

# Wait for update to complete
aws cloudformation wait stack-update-complete \
  --stack-name covey-weekly-job-dev \
  --region us-west-2
```

## Production Deployment

For production (staging/prod environments):

```bash
# Copy parameters but change Environment to 'prod'
aws cloudformation deploy \
  --template-file infrastructure/eventbridge-weekly-job.yaml \
  --stack-name covey-weekly-job-prod \
  --parameter-overrides \
    Environment=prod \
    LambdaFunctionArn=arn:aws:lambda:us-west-2:123456789012:function:covey-weekly-spot-prod \
    NotificationLambdaFunctionArn=arn:aws:lambda:us-west-2:123456789012:function:covey-notification-delivery-prod \
  --region us-west-2 \
  --capabilities CAPABILITY_NAMED_IAM
```

## Cleanup

To remove all resources:

```bash
aws cloudformation delete-stack \
  --stack-name covey-weekly-job-dev \
  --region us-west-2

# Wait for deletion
aws cloudformation wait stack-delete-complete \
  --stack-name covey-weekly-job-dev \
  --region us-west-2
```

## Related Documentation

- [Weekly Job Implementation Sequence](../docs/arch/weekly-job/implementation-sequence.md)
- [Notification Testing Guide](../docs/api/NOTIFICATION_TESTING.md)
- [AWS EventBridge Documentation](https://docs.aws.amazon.com/eventbridge/)
- [AWS CloudWatch Documentation](https://docs.aws.amazon.com/cloudwatch/)
