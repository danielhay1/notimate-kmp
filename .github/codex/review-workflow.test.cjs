const { test } = require('node:test');
const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');
const path = require('node:path');

const workflow = JSON.parse(execFileSync('ruby', ['-ryaml', '-rjson', '-e',
  'puts JSON.generate(YAML.load_file(ARGV[0]))',
  path.join(__dirname, '../workflows/codex-pr-review.yml')], { encoding: 'utf8' }));
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
const execute = (step, github, context, core = {}, env = {}, requireMock = require) =>
  new AsyncFunction('github', 'context', 'core', 'process', 'require', step.with.script)(
    github, context, core, { env }, requireMock);
const pr = { number: 1, state: 'open', head: { sha: 'head' }, base: { sha: 'base' }, changed_files: 1 };
const context = { repo: { owner: 'owner', repo: 'repo' }, payload: { pull_request: pr },
  serverUrl: 'https://github.com', runId: 1 };
const file = { filename: 'example.kt', status: 'modified', changes: 2,
  patch: '@@ -10,1 +10,1 @@\n-old\n+new' };
const approved = { verdict: 'APPROVE', scope_reviewed: 'fixture', findings: [],
  rejected_concerns: [], validation_assessment: 'fixture', merge_assessment: 'fixture' };
function mock(current = pr) {
  const calls = [];
  const record = name => async value => { calls.push([name, value]); return { data: {} }; };
  const github = { rest: {
    pulls: { get: async () => ({ data: current }), listFiles: 'files',
      listReviewComments: 'reviews', createReview: record('review') },
    issues: { listComments: 'comments', createComment: record('summary'), updateComment: record('summary') },
    repos: { createCommitStatus: record('status') },
  }, paginate: async method => method === 'files' ? [file] : [] };
  return { github, calls };
}
const publish = workflow.jobs.publish.steps[0];
const final = workflow.jobs.publish.steps[1];

test('privileged workflow uses trusted base and protected environment', () => {
  // Ruby Psych uses YAML 1.1, where the unquoted GitHub key `on` becomes true.
  assert.ok((workflow.on || workflow.true).pull_request_target);
  assert.equal(workflow.jobs.review.environment, 'codex-review');
  assert.equal(workflow.jobs.review.needs, 'pending');
  const steps = workflow.jobs.review.steps;
  assert.equal(steps[0].with.ref, '${{ github.event.pull_request.base.sha }}');
  assert.equal(steps[0].with['persist-credentials'], false);
  assert.equal(steps.at(-1).with['codex-version'], '0.157.1');
  assert.equal(steps.at(-1).with.sandbox, 'read-only');
  assert.equal(workflow.jobs.review.permissions.statuses, undefined);
});

test('rerun replaces previous success with pending before model execution', async () => {
  const { github, calls } = mock();
  await execute(workflow.jobs.pending.steps[0], github, context);
  assert.equal(calls.at(-1)[1].state, 'pending');
  assert.equal(calls.at(-1)[1].sha, 'head');
});

test('PR input stays JSON data and preserves head patch coordinates', async () => {
  const { github } = mock();
  let saved;
  await execute(workflow.jobs.review.steps[1], github, context, {}, {}, () => ({
    mkdirSync() {}, writeFileSync(name, data) { assert.equal(name, '.review-input/pr.json'); saved = JSON.parse(data); },
  }));
  assert.equal(saved.head, 'head');
  assert.equal(saved.files[0].patch, file.patch);
});

test('stale PR is rejected before publishing', async () => {
  const { github, calls } = mock({ ...pr, head: { sha: 'new-head' } });
  await assert.rejects(execute(publish, github, context, {}, {
    REVIEW_RESULT: JSON.stringify(approved), REVIEW_JOB_RESULT: 'success',
  }), /PR changed/);
  assert.deepEqual(calls, []);
});

test('missing patch fails closed before model invocation', async () => {
  const { github } = mock();
  github.paginate = async () => [{ ...file, patch: undefined }];
  await assert.rejects(execute(workflow.jobs.review.steps[1], github, context), /Incomplete PR patch/);
});

test('renamed content edits require a patch; pure renames are allowed', async () => {
  for (const changes of [0, 2]) {
    const { github } = mock();
    github.paginate = async () => [{ ...file, status: 'renamed', changes, patch: undefined }];
    const run = execute(workflow.jobs.review.steps[1], github, context, {}, {}, () => ({
      mkdirSync() {}, writeFileSync() {},
    }));
    if (changes) await assert.rejects(run, /Incomplete PR patch/);
    else await run;
  }
});

for (const [name, review, failed] of [
  ['valid approval', approved, false],
  ['malformed approval', { verdict: 'APPROVE' }, true],
  ['invalid diff coordinate', { ...approved, findings: [{ severity: 'nit', title: 'fixture', body: 'fixture',
    path: 'example.kt', line: 999, side: 'RIGHT' }] }, true],
  ['blocker cannot approve', { ...approved, findings: [{ severity: 'blocker', title: 'fixture', body: 'fixture',
    path: 'example.kt', line: 10, side: 'RIGHT' }] }, true],
]) {
  test(name, async () => {
    const { github } = mock();
    let actual = false;
    await execute(publish, github, context, { setFailed() { actual = true; } }, {
      REVIEW_RESULT: JSON.stringify(review), REVIEW_JOB_RESULT: 'success',
    });
    assert.equal(actual, failed);
  });
}

test('inline comment uses head SHA and PR patch line, not base workspace line', async () => {
  const { github, calls } = mock();
  await execute(publish, github, context, { setFailed() {} }, {
    REVIEW_JOB_RESULT: 'success', REVIEW_RESULT: JSON.stringify({ ...approved, findings: [
      { severity: 'nit', title: 'fixture', body: 'fixture', path: 'example.kt', line: 10, side: 'RIGHT' },
    ] }),
  });
  const review = calls.find(([name]) => name === 'review')[1];
  assert.equal(review.commit_id, 'head');
  assert.equal(review.comments[0].line, 10);
  assert.equal(review.event, 'COMMENT');
});

test('failed publisher or changed base cannot produce green status', async () => {
  for (const [current, result] of [[pr, 'failure'], [{ ...pr, base: { sha: 'new-base' } }, 'success']]) {
    const { github, calls } = mock(current);
    await execute(final, github, context, {}, { PUBLISH_RESULT: result });
    assert.equal(calls.at(-1)[1].state, 'failure');
  }
});
