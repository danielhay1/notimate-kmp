import assert from "node:assert/strict";
import { execFileSync, spawnSync } from "node:child_process";
import { randomBytes } from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

test("scans a synthetic secret after commit 30 even when removed at head", () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "notimate-secret-range-"));
  const git = (...args) => execFileSync("git", args, { cwd: directory, encoding: "utf8" }).trim();
  try {
    git("init", "--quiet");
    git("config", "user.name", "Synthetic test");
    git("config", "user.email", "synthetic@example.invalid");
    git("commit", "--quiet", "--allow-empty", "-m", "base");
    const base = git("rev-parse", "HEAD");
    for (let commit = 1; commit <= 30; commit++) {
      git("commit", "--quiet", "--allow-empty", "-m", `synthetic ${commit}`);
    }
    // Construct a nonfunctional test value at runtime; never publish this fixture.
    fs.writeFileSync(path.join(directory, "fixture.txt"), `github_token=ghp_${randomBytes(18).toString("hex")}\n`);
    git("add", "fixture.txt");
    git("commit", "--quiet", "-m", "synthetic detection fixture");
    fs.writeFileSync(path.join(directory, "fixture.txt"), "removed synthetic value\n");
    git("add", "fixture.txt");
    git("commit", "--quiet", "-m", "remove fixture");
    const result = spawnSync("bash", [fileURLToPath(new URL("scan-pr-secrets.sh", import.meta.url))], {
      cwd: directory,
      env: { ...process.env, BASE_SHA: base, HEAD_SHA: git("rev-parse", "HEAD") },
      encoding: "utf8",
    });
    assert.equal(result.status, 1, "The full history scan must detect the removed synthetic secret.");
    assert.match(result.stderr, /leaks found/);

    const mainBranch = git("branch", "--show-current");
    git("checkout", "--quiet", "-b", "synthetic-side", base);
    git("commit", "--quiet", "--allow-empty", "-m", "side change");
    git("checkout", "--quiet", mainBranch);
    git("merge", "--quiet", "--no-ff", "--no-commit", "synthetic-side");
    fs.writeFileSync(path.join(directory, "merge-only.txt"), `github_token=ghp_${randomBytes(18).toString("hex")}\n`);
    git("add", "merge-only.txt");
    git("commit", "--quiet", "-m", "synthetic merge resolution");
    const mergeParent = git("rev-parse", "HEAD^1");
    const mergeResult = spawnSync("bash", [fileURLToPath(new URL("scan-pr-secrets.sh", import.meta.url))], {
      cwd: directory,
      env: { ...process.env, BASE_SHA: mergeParent, HEAD_SHA: git("rev-parse", "HEAD") },
      encoding: "utf8",
    });
    assert.equal(mergeResult.status, 1, "The scan must detect a secret introduced only by a merge commit.");
    assert.match(mergeResult.stderr, /leaks found/);
  } finally {
    fs.rmSync(directory, { recursive: true, force: true });
  }
});
