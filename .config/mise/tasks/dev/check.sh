#!/usr/bin/env bash
#MISE description="Run full monorepo validation across static analysis, Konsist, and unit test suites"
#MISE alias="check"
#MISE depends=["dev:lint", "records:test", "frontend:test"]

echo "✅ Full monorepo validation passed: Detekt, Konsist, records tests, and frontend tests."
