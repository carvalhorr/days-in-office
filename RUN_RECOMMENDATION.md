# Winning experiment run — recommendation memo (DEPLOYMENT T1)

**Date:** 2026-05-22
**Decision:** Promote `runs/claude/sonnet-4-6/` into the product `app/`.

## Numbers

| Run | Main src files | Tasks DONE | Notes |
|---|---|---|---|
| `runs/claude/sonnet-4-6/` | 98 | 43 / 43 | All planned + extension tasks landed; smoke green except BUG-025 guard |
| `runs/aider/gemma4-31b/` | 19 | 2 | Stalled early |
| `runs/goose/gemma4-31b/` | 19 | 2 | Same model, similar stall |
| `runs/aider/devstral/` | 5 | 0 | Did not produce a buildable app |
| `runs/aider/qwen25coder-14b/` | 5 | 0 | "" |
| `runs/aider/deepseek-r1-32b/` | 0 | 0 | Did not start |
| `runs/aider/qwen25coder-32b/` | 0 | 0 | "" |
| `runs/goose/devstral/` | 0 | 0 | "" |
| `runs/openhands/gemma4-31b/` | 0 | 0 | "" |

## Smoke status of the winning run

Latest entry in `runs/claude/sonnet-4-6/SMOKE_RESULTS.md`:
`TASK-043 | 2026-05-20T20:53:14 | FAIL | e02_settingsWifiConnectedRowOpensAndEnablesWithoutCrash`

That one failing test is the BUG-025 regression guard (Wi-Fi SSID
`<unknown ssid>`). DEPLOYMENT.md explicitly allows shipping with Wi-Fi
detection deferred to a 1.x patch. BUG-024 (detector stale-config + missing
runtime permission requests) is fixed in this run.

## Recommendation

Adopt `runs/claude/sonnet-4-6/` as the product app source. No further survey
work needed — the gap to the runner-up is order-of-magnitude. Proceeding to
T2 (SPLIT_PLAN.md).
