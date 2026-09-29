import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import * as fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';

const asyncFunction = Object.getPrototypeOf(async function () {}).constructor;
const workflow = readFileSync(new URL('../workflows/dependency-submission.yml', import.meta.url), 'utf8');
const ci = readFileSync(new URL('../workflows/ci.yml', import.meta.url), 'utf8');

function inlineScript(source, step) {
  const lines = source.split('\n');
  const start = lines.findIndex(line => line.includes(`name: ${step}`));
  assert.ok(start >= 0);
  const script = lines.findIndex((line, index) => index > start && line.trim() === 'script: |');
  const result = [];
  for (const line of lines.slice(script + 1)) {
    if (line.trim() && !line.startsWith('            ')) break;
    result.push(line.slice(12));
  }
  return new asyncFunction('github', 'context', 'process', 'require', 'Buffer', result.join('\n'));
}

const submit = inlineScript(workflow, 'Validate and submit both snapshots');
const coverage = inlineScript(ci, 'Require snapshots for both compared revisions');
const base = 'a'.repeat(40);
const head = 'b'.repeat(40);

async function fixture(change = () => {}, stale = false) {
  const directory = fs.realpathSync(mkdtempSync(path.join(os.tmpdir(), 'notimate-dependencies-')));
  const root = path.join(directory, 'snapshots/dependency-graph-reports');
  const submitted = [];
  try {
    for (const [revision, sha] of [['base', base], ['head', head]]) {
      mkdirSync(path.join(root, revision), { recursive: true });
      const snapshot = {
        sha, detector: { name: 'Gradle', version: 'test', url: 'https://gradle.org' },
        scanned: '2026-09-30T00:00:00Z',
        manifests: { 'build.gradle.kts': { resolved: { example: { package_url: 'pkg:maven/example/library@1.0' } } } },
      };
      change(snapshot, revision);
      writeFileSync(path.join(root, revision, 'snapshot.json'), JSON.stringify(snapshot));
    }
    await submit({
      rest: { pulls: { get: async () => ({ data: { state: 'open', base: { sha: base }, head: { sha: stale ? 'c'.repeat(40) : head } } }) } },
      request: async (_route, snapshot) => submitted.push(snapshot),
    }, { repo: { owner: 'example', repo: 'project' }, payload: { workflow_run: { id: 123 } } }, {
      env: { BASE_SHA: base, HEAD_SHA: head, BASE_REF: 'dev', PR_NUMBER: '4' },
    }, name => name === 'node:fs' ? fs : { ...path, resolve: () => root }, Buffer);
    return submitted;
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

test('publishes exactly base/head data with trusted refs and a shared correlator', async () => {
  const submitted = await fixture(snapshot => { snapshot.ref = 'untrusted'; snapshot.owner = 'untrusted'; });
  assert.deepEqual(submitted.map(item => item.sha), [base, head]);
  assert.deepEqual(submitted.map(item => item.ref), ['refs/heads/dev', 'refs/pull/4/head']);
  assert.ok(submitted.every(item => item.owner === 'example' && item.job.correlator === 'notimate-gradle'));
  assert.ok(submitted.every(item => item.manifests['notimate-gradle'].name === 'notimate-gradle'));
});

test('refuses wrong snapshot SHA before publishing', async () => {
  await assert.rejects(fixture((snapshot, revision) => { if (revision === 'head') snapshot.sha = base; }), /revision/);
});

test('refuses snapshots without resolved Maven coverage', async () => {
  await assert.rejects(fixture(snapshot => { snapshot.manifests = {}; }), /coverage/);
});

test('refuses publication after the PR head changes', async () => {
  await assert.rejects(fixture(undefined, true), /changed/);
});

test('missing snapshot warning blocks even an empty successful comparison', async () => {
  await assert.rejects(coverage({ request: async () => ({ data: [], headers: {
    'x-github-dependency-graph-snapshot-warnings': Buffer.from('No snapshot for head SHA').toString('base64'),
  } }) }, { repo: {}, payload: { pull_request: { base: { sha: base }, head: { sha: head } } } }, {}, null, Buffer), /missing/);
});

test('an empty comparison with complete snapshots is valid', async () => {
  await coverage({ request: async () => ({ data: [], headers: {} }) }, {
    repo: {}, payload: { pull_request: { base: { sha: base }, head: { sha: head } } },
  }, {}, null, Buffer);
});
