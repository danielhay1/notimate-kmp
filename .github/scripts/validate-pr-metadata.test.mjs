import assert from "node:assert/strict";
import test from "node:test";

import { validateEvent, validatePullRequest } from "./validate-pr-metadata.mjs";

const validBody = `
## Summary

Replace an authenticated review job with deterministic checks.

## Validation

Node tests and Android verification passed.

## Risks

Agent review remains a procedural gate.
`;

test("accepts valid Conventional Commit metadata", () => {
  assert.deepEqual(
    validatePullRequest({
      title: "refactor(ci): separate deterministic and agent reviews",
      body: validBody,
    }),
    [],
  );
});

test("ignores template comments before supplied content", () => {
  assert.deepEqual(
    validatePullRequest({
      title: "ci(github): validate pull request metadata",
      body: `
## Summary
<!-- Explain what changes and why. -->
Validate metadata before merge.
## Validation
<!-- List observed commands/results. -->
Node tests passed.
## Risks
<!-- Describe limitations. -->
None identified.
`,
    }),
    [],
  );
});

test("requires a pull-request event", () => {
  assert.deepEqual(validateEvent({}), [
    "The GitHub event does not contain pull-request metadata.",
  ]);
});

test("rejects malformed and overly long titles", () => {
  const errors = validatePullRequest({
    title: `Update ${"workflow ".repeat(10)}`,
    body: validBody,
  });
  assert.equal(errors.length, 2);
});

test("requires meaningful content below every heading", () => {
  const errors = validatePullRequest({
    title: "ci(github): validate pull request metadata",
    body: `
## Summary
<!-- Fill this in. -->
## Validation
Not run
## Risks
`,
  });
  assert.deepEqual(errors, [
    "Provide a non-empty ## Summary section.",
    "Provide a non-empty ## Risks section.",
    "Explain why validation was not run after 'Not run:'.",
  ]);
});

test("rejects unexplained skipped validation in Markdown lists", () => {
  for (const validation of [
    "- Not run",
    "* Not run",
    "  +   Not run  ",
    "1. Not run",
    "1) Not run",
    "- [ ] Not run",
  ]) {
    const errors = validatePullRequest({
      title: "ci(github): validate pull request metadata",
      body: validBody.replace("Node tests and Android verification passed.", validation),
    });
    assert.deepEqual(errors, ["Explain why validation was not run after 'Not run:'."]);
  }
});

test("accepts an explained skipped validation list item", () => {
  for (const validation of [
    "- Not run: documentation-only change.",
    "1. Not run: documentation-only change.",
    "- [ ] Not run: documentation-only change.",
  ]) {
    assert.deepEqual(
      validatePullRequest({
        title: "docs(ci): document pull request metadata",
        body: validBody.replace("Node tests and Android verification passed.", validation),
      }),
      [],
    );
  }
});

test("treats metadata as text rather than executable input", () => {
  const errors = validatePullRequest({
    title: "ci(github): validate literal command syntax",
    body: `${validBody}\n\`\`\`text\n$(touch /tmp/notimate-metadata-test)\n\`\`\``,
  });
  assert.deepEqual(errors, []);
});
