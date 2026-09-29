import assert from "node:assert/strict";
import fs from "node:fs";
import test from "node:test";

const workflow = fs.readFileSync(
  new URL("../workflows/ci.yml", import.meta.url),
  "utf8",
);

test("uses pull-request events without privileged credentials", () => {
  assert.match(workflow, /\n  pull_request:\n/);
  assert.match(workflow, /types: \[opened, reopened, synchronize, ready_for_review, edited\]/);
  assert.doesNotMatch(workflow, /pull_request_target|OPENAI_API_KEY|secrets\./);
  assert.doesNotMatch(workflow, /^\s*[\w-]+:\s*write\s*$/m);
});

test("defines every required deterministic check", () => {
  for (const name of [
    "CI / Android MVP",
    "CI / PR policy",
    "CI / Dependency review",
    "CI / Secret scan",
  ]) {
    assert.match(workflow, new RegExp(`name: ${name.replaceAll("/", "\\/")}`));
  }
});

test("pins every action and disables persisted checkout credentials", () => {
  const actions = [...workflow.matchAll(/^\s*uses:\s*(\S+)/gm)].map((match) => match[1]);
  assert.ok(actions.length > 0);
  for (const action of actions) {
    assert.match(action, /@[0-9a-f]{40}$/);
  }

  const checkouts = actions.filter((action) => action.startsWith("actions/checkout@"));
  const disabledCredentials = workflow.match(/persist-credentials: false/g) ?? [];
  assert.equal(disabledCredentials.length, checkouts.length);
});

test("keeps secret-scan publication disabled", () => {
  assert.match(workflow, /GITLEAKS_ENABLE_COMMENTS: "false"/);
  assert.match(workflow, /GITLEAKS_ENABLE_SUMMARY: "false"/);
  assert.match(workflow, /GITLEAKS_ENABLE_UPLOAD_ARTIFACT: "false"/);
});
