# Dev Onboarding — PMKT pmkt-shared

> Quy trình setup máy dev cho 6 service + pmkt-shared. T1.9 (Batch 2).

## Prerequisites

| Tool | Version | Cài |
|---|---|---|
| Java | 21 LTS (Temurin or OpenJDK) | `brew install openjdk@21` |
| Maven | 3.9.16+ | `brew install maven` |
| Docker | latest (cho Testcontainers) | Docker Desktop |
| git | 2.40+ | `brew install git` |

Set `JAVA_HOME` + PATH ở `~/.zshrc`:

```bash
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
```

Verify: `java -version` → `openjdk version "21.0.x"`.

## Clone + git hook setup

```bash
git clone https://github.com/thanhpt-osp/pmkt-shared.git
cd pmkt-shared
./scripts/install-hooks.sh
```

Sau bước này, mọi `git commit` sẽ tự động:
1. Chạy `mvn spotless:apply` trên file Java đã stage
2. Re-stage file đã format
3. Cho commit tiếp tục

Bypass tạm: `git commit --no-verify` (dùng dè dặt — CI sẽ FAIL nếu code không pass Spotless + Checkstyle).

## Build + test

```bash
# Full build (cần Docker daemon cho Testcontainers)
mvn -B clean verify

# Skip test
mvn -B clean install -DskipTests

# Chỉ test 1 module
mvn -B -pl pmkt-shared-libs test
```

## Maven repo — GitHub Packages

`pmkt-shared` artifact (com.dopai.pmkt:pmkt-shared-libs / pmkt-shared-bom / pmkt-shared) publish tới
[GitHub Packages](https://github.com/thanhpt-osp/pmkt-shared/packages).

Service repo (pmkt-core-service, etc.) bootstrap qua **clone+install local m2** trong CI (xem
`reusable-build.yml`). Dev local cũng cần install pmkt-shared trước:

```bash
cd ~/path/to/pmkt-shared
mvn -B clean install -DskipTests   # cài vào ~/.m2/repository
```

## IDE setup

- **IntelliJ IDEA**: import as Maven project. Enable annotation processing (Settings → Build → Compiler → Annotation Processors → Enable). Install plugin **Spotless Gradle** (chỉ optional cho real-time format).
- **VS Code**: install extension pack **Extension Pack for Java**. Reload IDE sau khi clone.

## Style check on demand

```bash
# Auto-fix Spotless trên toàn repo
mvn spotless:apply

# Verify Spotless không cần thay đổi (CI gate)
mvn spotless:check

# Checkstyle (chỉ check, không fix)
mvn checkstyle:check
```

## Troubleshooting

| Vấn đề | Khắc phục |
|---|---|
| `mvn checkstyle:check` báo `ConstantName` | Static final field phải UPPER_SNAKE_CASE — rename hoặc xem coding-style.md §2 |
| Testcontainers timeout | Verify Docker daemon: `docker ps` |
| GH Packages 401 | `gh auth refresh -h github.com -s read:packages` |
| Spotless reformat liên tục | Lock `google-java-format.version` ở parent pom = 1.22.0 |

## Liên quan

- [coding-style.md](rules/coding-style.md) — Style chi tiết
- [architecture-rules.md](rules/architecture-rules.md) — Layer + boundary rule
- [README.md](../README.md) — Overview pmkt-shared
