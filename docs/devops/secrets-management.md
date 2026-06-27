# Secrets Management for Covey

**Guidelines for storing, managing, and rotating secrets across GitHub, AWS, and local development.**

---

## Overview

**Golden Rule**: Never commit secrets to the repository.

Secrets are stored in three places depending on context:

| Secret | Storage | Usage | Rotation |
|--------|---------|-------|----------|
| AWS credentials | AWS IAM (OIDC) | GitHub Actions | Auto-rotated by AWS |
| Firebase service account | AWS Secrets Manager | Lambda runtime | 90 days |
| Google Places API key | AWS Secrets Manager | Lambda runtime | 90 days |
| iOS signing cert | GitHub Secrets | GitHub Actions (TestFlight) | Annual (Apple) |
| Local dev credentials | `.env.local` (git-ignored) | Local development | As needed |

---

## 1. GitHub Secrets (CI/CD Environment Variables)

GitHub Secrets are injected into CI/CD workflows and are not visible in logs.

### Setting Up GitHub Secrets

1. Go to repository → **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Enter `Name` and `Value`
4. Click **Add secret**

### Required GitHub Secrets

#### AWS Credentials (OIDC)

Modern approach: Use OpenID Connect (OIDC) instead of static credentials.

**Setup** (one-time):
```bash
# Create GitHub OIDC identity provider in AWS IAM
# See: https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/about-security-hardening-with-openid-connect

aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list <GitHub thumbprint>

# Create IAM role for GitHub Actions
aws iam create-role \
  --role-name GitHubActionsRole \
  --assume-role-policy-document file://trust-policy.json
```

**GitHub Secret** (store AWS account IDs only, not credentials):
- `AWS_ACCOUNT_ID`: `123456789012` (dev account)
- `AWS_ACCOUNT_ID_PROD`: `987654321098` (prod account)

#### iOS Signing (Apple)

**Generate signing certificate**:
```bash
# Export p12 certificate from Keychain
# Keychain Access → My Certificates → right-click cert → Export
# Save as `Certificates.p12`

# Convert to base64
cat Certificates.p12 | base64 | pbcopy

# Paste into GitHub Secrets as `IOS_SIGNING_CERT_BASE64`
```

**GitHub Secrets**:
- `IOS_SIGNING_CERT_BASE64`: Base64-encoded p12 file
- `IOS_SIGNING_CERT_PASSWORD`: Password used during p12 export
- `IOS_PROVISIONING_PROFILE_BASE64`: Base64-encoded provisioning profile (.mobileprovision)

#### App Store Connect API

**Generate API key** (in App Store Connect):
1. Users and Access → Integrations → Keys
2. Click "Generate API Key"
3. Select "Admin" role
4. Download the private key

**GitHub Secrets**:
- `APPSTORE_ISSUER_ID`: From App Store Connect (Integrations → Apps → Issuer ID)
- `APPSTORE_API_KEY_ID`: From downloaded key
- `APPSTORE_API_PRIVATE_KEY`: Base64-encode the private key (.p8 file)

```bash
cat AuthKey_*.p8 | base64 | pbcopy
```

#### Slack Webhook (Notifications)

**Generate webhook**:
1. Go to Slack workspace
2. Create a new app (or use existing)
3. Enable Incoming Webhooks
4. Create a webhook for `#deployments` channel
5. Copy webhook URL

**GitHub Secret**:
- `SLACK_WEBHOOK_URL`: https://hooks.slack.com/services/T00.../B00.../XXXX

---

## 2. AWS Secrets Manager (Runtime Secrets)

Lambda functions retrieve secrets from AWS Secrets Manager at runtime.

### Setting Up AWS Secrets

#### Google Places API Key

```bash
aws secretsmanager create-secret \
  --name google-places-dev \
  --secret-string '{"api-key":"AIza..."}' \
  --region us-west-2 \
  --tags Key=Environment,Value=dev

aws secretsmanager create-secret \
  --name google-places-prod \
  --secret-string '{"api-key":"AIza..."}' \
  --region us-west-2 \
  --tags Key=Environment,Value=prod
```

#### Firebase Admin SDK Service Account

```bash
# Download service account JSON from Firebase Console
# Service Accounts → Generate New Private Key

aws secretsmanager create-secret \
  --name firebase-admin-dev \
  --secret-string file://firebase-dev-key.json \
  --region us-west-2 \
  --tags Key=Environment,Value=dev

aws secretsmanager create-secret \
  --name firebase-admin-prod \
  --secret-string file://firebase-prod-key.json \
  --region us-west-2 \
  --tags Key=Environment,Value=prod
```

### Lambda IAM Policy (Least Privilege)

Attach this policy to the Lambda execution role:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:us-west-2:123456789012:secret:google-places-dev-*",
        "arn:aws:secretsmanager:us-west-2:123456789012:secret:firebase-admin-dev-*"
      ],
      "Condition": {
        "StringEquals": {
          "aws:RequestedRegion": "us-west-2"
        }
      }
    }
  ]
}
```

### Java Code to Retrieve Secrets

```java
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class SecretsUtil {
    private static final String REGION = "us-west-2";
    private static SecretsManagerClient client = SecretsManagerClient.builder()
        .region(Region.of(REGION))
        .build();

    public static String getSecret(String secretName) {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
            
            GetSecretValueResponse response = client.getSecretValue(request);
            return response.secretString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve secret: " + secretName, e);
        }
    }

    // Usage
    public static void main(String[] args) {
        String apiKey = getSecret("google-places-dev");
        System.out.println("API Key: " + apiKey);
    }
}
```

---

## 3. Local Development Secrets

### `.env.local` Setup

Create `.env.local` in the monorepo root (never commit this):

```bash
# Backend (Java)
FIREBASE_PROJECT_ID=covey-dev
FIREBASE_EMULATOR_HOST=localhost:9099
GOOGLE_PLACES_API_KEY=AIzaSyD...
AWS_REGION=us-west-2

# iOS
FIREBASE_PROJECT_ID=covey-dev
API_BASE_URL=http://localhost:3000
LOG_LEVEL=debug

# Local Firebase Emulator
FIRESTORE_EMULATOR_HOST=localhost:8080
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

### Using `.env.local` in Backend

**Gradle**:
```gradle
task loadEnv {
    doFirst {
        file('.env.local').readLines().forEach { line ->
            if (!line.isEmpty() && !line.startsWith('#')) {
                def (key, value) = line.split('=')
                System.setProperty(key, value)
            }
        }
    }
}

build.dependsOn loadEnv
```

**Maven**:
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <phase>initialize</phase>
            <goals>
                <goal>exec</goal>
            </goals>
            <configuration>
                <executable>./load-env.sh</executable>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Using `.env.local` in iOS

**Swift**:
```swift
import Foundation

class ConfigManager {
    static let shared = ConfigManager()
    
    let firebaseProjectId: String
    let apiBaseUrl: String
    
    init() {
        if let path = Bundle.main.path(forResource: ".env.local", ofType: "") {
            let content = try? String(contentsOfFile: path)
            let lines = content?.split(separator: "\n") ?? []
            
            var config = [String: String]()
            for line in lines {
                let parts = line.split(separator: "=", maxSplits: 1)
                if parts.count == 2 {
                    config[String(parts[0])] = String(parts[1])
                }
            }
            
            self.firebaseProjectId = config["FIREBASE_PROJECT_ID"] ?? "covey-dev"
            self.apiBaseUrl = config["API_BASE_URL"] ?? "http://localhost:3000"
        } else {
            // Fallback to defaults
            self.firebaseProjectId = "covey-dev"
            self.apiBaseUrl = "http://localhost:3000"
        }
    }
}
```

---

## 4. Secret Rotation

### Google Places API Key

**Rotation schedule**: Every 90 days

**Steps**:
1. Generate new key in Google Cloud Console
2. Update in AWS Secrets Manager: `aws secretsmanager update-secret --secret-id google-places-dev --secret-string '{"api-key":"AIza..."}'`
3. Lambda auto-retrieves new key on next invocation (no Lambda restart needed)
4. Revoke old key in Google Cloud Console

### Firebase Admin SDK Key

**Rotation schedule**: Every 90 days

**Steps**:
1. Download new private key from Firebase Console
2. Update in AWS Secrets Manager: `aws secretsmanager update-secret --secret-id firebase-admin-dev --secret-string file://new-key.json`
3. Lambda auto-retrieves new key on next invocation
4. Revoke old key in Firebase Console

### iOS Signing Certificate

**Rotation schedule**: Annually (Apple requirement)

**Steps**:
1. Generate new certificate in Apple Developer
2. Export as p12 and base64-encode
3. Update GitHub Secret `IOS_SIGNING_CERT_BASE64`
4. Update GitHub Secret `IOS_SIGNING_CERT_PASSWORD` if password changed
5. Next TestFlight build will use new cert

---

## 5. Secret Scanning

### GitHub Secret Scanning

Enable native GitHub secret scanning (detects accidental commits):

1. Go to repository → **Settings** → **Security**
2. Enable **Secret scanning**
3. GitHub automatically scans all commits for known secret patterns

### Pre-Commit Hook (Local Protection)

Prevent accidental secret commits using `git-secrets`:

```bash
# Install git-secrets
brew install git-secrets

# Clone Covey repo
git clone https://github.com/williamchknight/covey.git
cd covey

# Install git-secrets hook
git secrets --install
git secrets --register-aws
git secrets --add '.env.local'
git secrets --add 'firebase.*\.json'
git secrets --add '.*\.p12'
git secrets --add '.*\.p8'

# git-secrets will prevent commits with secrets
```

### CI/CD Secret Scanning

The test workflow includes TruffleHog scanning:

```yaml
- name: TruffleHog Scan
  uses: trufflesecurity/trufflehog@main
  with:
    path: ./
    base: main
```

---

## 6. Incident Response

### If a Secret is Leaked

**Immediate actions**:
1. Revoke the secret immediately (Google Cloud, Firebase, AWS)
2. Create new secret
3. Update in GitHub Secrets / AWS Secrets Manager
4. Redeploy affected services
5. Audit logs: `aws cloudtrail lookup-events --lookup-attributes AttributeKey=ResourceName,AttributeValue=<secret-arn>`

**Notification**:
- Notify the team in Slack `#security`
- Document in incident report
- No need to notify users unless customer data was exposed

---

## Checklist for New Developers

When onboarding a new developer:

- [ ] Grant GitHub team access to the repo
- [ ] GitHub Secrets are auto-available (no manual setup needed)
- [ ] Provide `.env.local` template (without secret values)
- [ ] Provide instructions to set up Firebase Emulator
- [ ] Provide instructions for running local Lambda (SAM CLI)
- [ ] Remind: **Never commit secrets, never log secrets, never paste secrets in Slack**

---

## References

- [GitHub Actions: Using Secrets](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [Git Secrets](https://github.com/awslabs/git-secrets)
- [TruffleHog](https://github.com/trufflesecurity/trufflehog)
