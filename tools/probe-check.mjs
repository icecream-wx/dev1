#!/usr/bin/env node
// 批次校验脚本：确认 docs/probe.md 与 README.md 都包含本批次标识。
// 无第三方依赖，Node.js 直接运行。
import { readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const BATCH_ID = 'B2026-1106-PROBE';
const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');

const targets = [
  { name: 'docs/probe.md', path: join(projectRoot, 'docs', 'probe.md') },
  { name: 'README.md', path: join(projectRoot, 'README.md') },
];

const missing = [];

for (const target of targets) {
  let content;
  try {
    content = readFileSync(target.path, 'utf8');
  } catch (error) {
    missing.push(`${target.name}（读取失败：${error.code ?? error.message}）`);
    continue;
  }
  if (!content.includes(BATCH_ID)) {
    missing.push(`${target.name}（缺少批次标识 ${BATCH_ID}）`);
  }
}

if (missing.length > 0) {
  console.error(`probe failed: 未通过批次标识校验 ${BATCH_ID}`);
  for (const item of missing) {
    console.error(`- ${item}`);
  }
  process.exit(1);
}

console.log('probe ok');
process.exit(0);
