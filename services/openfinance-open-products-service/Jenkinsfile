pipeline {
    agent any
    options { timestamps() }

    environment {
        SERVICE_DIR = 'services/openfinance-open-products-service'
        IMAGE_NAME = 'openfinance-open-products-service:' + (env.GIT_COMMIT ?: 'local')
        STRICT_DEPRECATED_ROOTS = 'true'
    }

    stages {
        stage('Validate Workspace') {
            steps {
                sh '''
                  set -euo pipefail
                  test -f "${SERVICE_DIR}/build.gradle"
                  test -x ./gradlew
                '''
            }
        }
        stage('Repository Governance') {
            steps {
                sh '''
                  set -euo pipefail
                  export STRICT_DEPRECATED_ROOTS="${STRICT_DEPRECATED_ROOTS:-true}"
                  bash tools/validation/validate-repo-governance.sh

                  python3 -m venv .venv-validation
                  . .venv-validation/bin/activate
                  python -m pip install --upgrade pip coverage
                  python -m coverage run --source=tools/validation -m unittest discover -s tools/validation/tests -p 'test_*.py'
                  python -m coverage report --include='*/repo_governance_validator.py' --fail-under=90
                '''
            }
        }
        stage('Quality Gate') {
            steps {
                sh '''
                  set -euo pipefail
                  ./gradlew -p "${SERVICE_DIR}" --no-daemon clean check
                '''
            }
        }
        stage('Security Gate') {
            steps {
                sh '''
                  set -euo pipefail
                  mkdir -p "${SERVICE_DIR}/build/reports/security"
                  ./gradlew -p "${SERVICE_DIR}" --no-daemon dependencies > "${SERVICE_DIR}/build/reports/security/dependencies.txt"
                  command -v trivy >/dev/null 2>&1
                  trivy fs --exit-code 1 --severity HIGH,CRITICAL "${SERVICE_DIR}"
                  command -v gitleaks >/dev/null 2>&1
                  gitleaks detect --no-git --source "${SERVICE_DIR}" --exit-code 1
                '''
            }
        }
        stage('Build Image') {
            steps {
                sh '''
                  set -euo pipefail
                  if command -v docker >/dev/null 2>&1; then
                    DOCKERFILE="${SERVICE_DIR}/infrastructure/Dockerfile"
                    if [ -f "${DOCKERFILE}" ]; then
                      docker build -t "${IMAGE_NAME}" -f "${DOCKERFILE}" "${SERVICE_DIR}"
                    else
                      docker build -t "${IMAGE_NAME}" "${SERVICE_DIR}"
                    fi
                  else
                    echo "docker not installed; skipping image build"
                  fi
                '''
            }
        }
        stage('Sign & Publish Image') {
            when {
                expression { return env.PUBLISH_IMAGE == 'true' }
            }
            steps {
                sh '''
                  set -euo pipefail

                  if command -v cosign >/dev/null 2>&1 && [ -n "${COSIGN_KEY:-}" ]; then
                    cosign sign --key "${COSIGN_KEY}" "${IMAGE_NAME}"
                  else
                    echo "cosign key not configured; skipping image signing"
                  fi

                  if command -v docker >/dev/null 2>&1 && [ -n "${DOCKER_REGISTRY:-}" ] && [ -n "${DOCKER_USERNAME:-}" ] && [ -n "${DOCKER_PASSWORD:-}" ]; then
                    echo "${DOCKER_PASSWORD}" | docker login "${DOCKER_REGISTRY}" --username "${DOCKER_USERNAME}" --password-stdin
                    docker push "${IMAGE_NAME}"
                  else
                    echo "registry credentials not configured; skipping image publish"
                  fi
                '''
            }
        }
    }
}
