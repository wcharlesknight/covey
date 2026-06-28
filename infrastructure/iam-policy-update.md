# IAM Policy Update: GitHubActionsRole

## Issue
Lambda deployment waiter was failing with `AccessDeniedException` - role missing `lambda:GetFunctionConfiguration` permission.

## Solution
Added `lambda:GetFunctionConfiguration` to GitHubActionsCoveyPolicy for the GitHubActionsRole.

## Permissions Added
- `lambda:GetFunctionConfiguration` - required by `aws lambda wait function-updated` command

## Resources Affected
- `arn:aws:lambda:us-west-2:534344665494:function:covey-weekly-spot-dev`
- `arn:aws:lambda:us-west-2:534344665494:function:covey-weekly-spot-prod`

## Applied Via
```bash
aws iam put-role-policy --role-name GitHubActionsRole --policy-name GitHubActionsCoveyPolicy --policy-document {...}
```

## Status
✅ Applied to AWS account 534344665494
