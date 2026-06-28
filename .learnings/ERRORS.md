# Errors

## [ERR-20260628-001] rtk-command-wrapper

**Logged**: 2026-06-28T20:33:00+08:00
**Priority**: low
**Status**: resolved
**Area**: config

### Summary
The shared agent instructions assume an `rtk` command wrapper that is not installed in this Windows environment.

### Error
```text
The term 'rtk' is not recognized as a name of a cmdlet, function, script file, or executable program.
```

### Context
- Command attempted: `rtk ntn whoami`
- Environment: PowerShell on Windows

### Suggested Fix
Use the underlying command directly when `Get-Command rtk` is unavailable.

### Metadata
- Reproducible: yes
- Related Files: C:\Users\19682\Documents\agent-skill-registry\agent-core.md

### Resolution
- **Resolved**: 2026-06-28T20:35:00+08:00
- **Notes**: Ran `ntn whoami` directly with a longer timeout.

---

## [ERR-20260628-005] review-test-progress-contract

**Logged**: 2026-06-28T20:43:00+08:00
**Priority**: low
**Status**: resolved
**Area**: tests

### Summary
The review service test did not provide card progress after answer evaluation became learning-mode aware.

### Error
```text
NoSuchElementException: No progress record for card: 10
```

### Context
- Production cards always have a progress row.
- The new evaluation path reads progress to choose `LEARNING` or `REVIEW` coaching.

### Suggested Fix
Mock the progress row and verify the mode-aware AI method.

### Metadata
- Reproducible: yes
- Related Files: src/test/java/com/memospark/core/service/ReviewServiceTest.java

### Resolution
- **Resolved**: 2026-06-28T20:44:00+08:00
- **Notes**: Updated the test and added a bounded first-review interval test.

---

## [ERR-20260628-003] apply-patch-context

**Logged**: 2026-06-28T20:39:00+08:00
**Priority**: low
**Status**: resolved
**Area**: backend

### Summary
A large `CardService` patch was rejected because one expected context line was incorrect.

### Error
```text
apply_patch verification failed: Failed to find expected lines
```

### Context
- No file changes were applied by the rejected patch.
- The patch was split into smaller import and method hunks.

### Suggested Fix
Inspect the exact file context and apply small hunks in dirty worktrees.

### Metadata
- Reproducible: no
- Related Files: src/main/java/com/memospark/core/service/CardService.java

### Resolution
- **Resolved**: 2026-06-28T20:40:00+08:00
- **Notes**: Applied the intended changes in verified smaller hunks.

---

## [ERR-20260628-004] ai-json-array-generic

**Logged**: 2026-06-28T20:41:00+08:00
**Priority**: medium
**Status**: resolved
**Area**: backend

### Summary
Backend compilation failed because `extractJsonArray` only accepted string-valued maps.

### Error
```text
List<Map<String,String>> cannot conform to List<Map<String,Object>>
```

### Context
- Card governance responses include numeric `cardId` and `order` values.
- The helper declared a generic type parameter but returned a fixed map type.

### Suggested Fix
Return the requested generic type from `extractJsonArray`, matching `extractJsonObject`.

### Metadata
- Reproducible: yes
- Related Files: src/main/java/com/memospark/core/service/AiService.java

### Resolution
- **Resolved**: 2026-06-28T20:42:00+08:00
- **Notes**: Changed the helper to `<T> T extractJsonArray(..., TypeReference<T>)`.

---

## [ERR-20260628-002] notion-whoami-timeout

**Logged**: 2026-06-28T20:34:00+08:00
**Priority**: low
**Status**: resolved
**Area**: config

### Summary
`ntn whoami` exceeded the default command timeout although the CLI and login were valid.

### Error
```text
command timed out after 14053 milliseconds
```

### Context
- Command attempted: `ntn whoami`
- The same command completed in 23.8 seconds with a 60-second timeout.

### Suggested Fix
Allow at least 60 seconds for `ntn` network-backed commands.

### Metadata
- Reproducible: unknown
- Related Files: C:\Users\19682\.codex\skills\notion\SKILL.md

### Resolution
- **Resolved**: 2026-06-28T20:35:00+08:00
- **Notes**: Retried with a 60-second timeout and confirmed the authenticated workspace.

---
