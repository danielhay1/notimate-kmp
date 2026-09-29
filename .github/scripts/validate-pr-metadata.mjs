import fs from "node:fs";
import process from "node:process";
import { fileURLToPath } from "node:url";

const TITLE_PATTERN =
  /^(build|chore|ci|docs|feat|fix|perf|refactor|revert|test)(\([a-z0-9][a-z0-9._/-]*\))?!?: [a-z0-9].+$/;
const REQUIRED_SECTIONS = ["Summary", "Validation", "Risks"];

function withoutComments(value) {
  return value.replace(/<!--[\s\S]*?-->/g, "").trim();
}

function sectionContent(body, heading) {
  const escaped = heading.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = body.match(
    new RegExp(
      `(?:^|\\n)##[ \\t]+${escaped}[ \\t]*\\r?\\n` +
        "([\\s\\S]*?)(?=\\r?\\n##[ \\t]+|$)",
      "i",
    ),
  );
  return match ? withoutComments(match[1]) : "";
}

export function validatePullRequest({ title = "", body = "" } = {}) {
  const errors = [];
  const normalizedTitle = title.trim();

  if (!TITLE_PATTERN.test(normalizedTitle)) {
    errors.push("Use a Conventional Commit title with a lowercase type and summary.");
  }
  if (normalizedTitle.length > 72) {
    errors.push("Keep the pull-request title at or below 72 characters.");
  }

  for (const heading of REQUIRED_SECTIONS) {
    if (!sectionContent(body, heading)) {
      errors.push(`Provide a non-empty ## ${heading} section.`);
    }
  }

  const validation = sectionContent(body, "Validation");
  const hasUnexplainedSkippedValidation = validation
    .split(/\r?\n/)
    .map((line) => line.trim().replace(/^[-*+]\s+/, ""))
    .some((line) => /^not run\b/i.test(line) && !/^not run\s*:\s*\S.+/i.test(line));
  if (hasUnexplainedSkippedValidation) {
    errors.push("Explain why validation was not run after 'Not run:'.");
  }

  return errors;
}

export function validateEvent(event) {
  if (!event?.pull_request) {
    return ["The GitHub event does not contain pull-request metadata."];
  }
  return validatePullRequest({
    title: event.pull_request.title,
    body: event.pull_request.body ?? "",
  });
}

function run() {
  const eventPath = process.argv[2];
  if (!eventPath) {
    throw new Error("Pass the GitHub event JSON path as the first argument.");
  }

  const event = JSON.parse(fs.readFileSync(eventPath, "utf8"));
  const errors = validateEvent(event);
  if (errors.length > 0) {
    for (const error of errors) {
      console.error(`::error::${error}`);
    }
    process.exitCode = 1;
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === fs.realpathSync(process.argv[1])) {
  run();
}
