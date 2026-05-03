#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -n "${MIYU_GIT_PUSH_ENV:-}" ]]; then
  ENV_FILE="$MIYU_GIT_PUSH_ENV"
elif [[ -s "$ROOT_DIR/.git-env-push" ]]; then
  ENV_FILE="$ROOT_DIR/.git-env-push"
else
  ENV_FILE="$ROOT_DIR/.git-push.env"
fi

if [[ ! -f "$ENV_FILE" ]]; then
  cat >&2 <<MSG
Missing $ENV_FILE.

Create it from the template:
  cp .git-push.env.example .git-push.env

Then fill in GITHUB_USERNAME, GITHUB_PAT, and GMAIL.
The alternate local filename .git-env-push is also supported.
MSG
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${GITHUB_USERNAME:?Set GITHUB_USERNAME in $ENV_FILE}"
: "${GITHUB_PAT:?Set GITHUB_PAT in $ENV_FILE}"

git_email="${GIT_EMAIL:-${GMAIL:-}}"
if [[ -z "$git_email" ]]; then
  echo "Set GMAIL or GIT_EMAIL in $ENV_FILE." >&2
  exit 1
fi

remote="${GIT_REMOTE:-origin}"
branch="${GIT_BRANCH:-}"

cd "$ROOT_DIR"

if [[ -z "$branch" ]]; then
  branch="$(git branch --show-current)"
fi

if [[ -z "$branch" ]]; then
  echo "Unable to determine the current branch. Set GIT_BRANCH in $ENV_FILE." >&2
  exit 1
fi

remote_url="$(git remote get-url "$remote")"
push_url=""

case "$remote_url" in
  https://github.com/*)
    push_url="$remote_url"
    ;;
  git@github.com:*)
    push_url="https://github.com/${remote_url#git@github.com:}"
    ;;
  *)
    echo "Remote '$remote' must point to github.com over HTTPS or SSH. Current URL: $remote_url" >&2
    exit 1
    ;;
esac

askpass="$(mktemp)"
cleanup() {
  rm -f "$askpass"
}
trap cleanup EXIT

cat > "$askpass" <<'ASKPASS'
#!/usr/bin/env bash
case "$1" in
  *Username*) printf '%s\n' "$GITHUB_USERNAME" ;;
  *Password*) printf '%s\n' "$GITHUB_PAT" ;;
  *) printf '\n' ;;
esac
ASKPASS
chmod 700 "$askpass"

GIT_ASKPASS="$askpass" \
GIT_TERMINAL_PROMPT=0 \
git \
  -c "user.name=$GITHUB_USERNAME" \
  -c "user.email=$git_email" \
  -c credential.helper= \
  -c "remote.$remote.pushurl=$push_url" \
  push "$remote" "$branch"
