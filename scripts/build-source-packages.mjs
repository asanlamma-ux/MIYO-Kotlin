import crypto from 'crypto';
import fs from 'fs';
import path from 'path';
import JSZip from 'jszip';

const ROOT = process.cwd();
const SOURCE_PLUGIN_ROOT = path.join(ROOT, 'source-plugins');
const PACKAGE_SOURCE_ROOT = path.join(SOURCE_PLUGIN_ROOT, 'packages');
const CATALOG_PATH = path.join(SOURCE_PLUGIN_ROOT, 'catalog.json');
const TEMPLATE_PATH = path.join(SOURCE_PLUGIN_ROOT, 'templates', 'source-package.js');
const DIST_ROOT = path.join(SOURCE_PLUGIN_ROOT, '.dist');
const DIST_PACKAGE_ROOT = path.join(DIST_ROOT, 'packages');
const APP_ASSET_ROOT = path.join(ROOT, 'app', 'src', 'main', 'assets', 'source-packages');
const MANIFEST_FILE = 'manifest.json';
const MAIN_FILE = 'main.js';
const PACKAGE_EXTENSION = '.miyuplugin.zip';

if (!fs.existsSync(CATALOG_PATH)) {
  throw new Error(`Missing source package catalog: ${CATALOG_PATH}`);
}

const catalog = JSON.parse(fs.readFileSync(CATALOG_PATH, 'utf8'))
  .filter(entry => entry && entry.enabled !== false);

const template = fs.existsSync(TEMPLATE_PATH)
  ? fs.readFileSync(TEMPLATE_PATH, 'utf8')
  : null;

fs.rmSync(DIST_ROOT, { recursive: true, force: true });
fs.mkdirSync(DIST_ROOT, { recursive: true });
fs.mkdirSync(DIST_PACKAGE_ROOT, { recursive: true });
fs.rmSync(APP_ASSET_ROOT, { recursive: true, force: true });
fs.mkdirSync(APP_ASSET_ROOT, { recursive: true });

const packageIds = new Set();
const providerIds = new Set();
const feed = [];

for (const spec of catalog) {
  validateSpec(spec, packageIds, providerIds);

  const manifest = {
    schemaVersion: 1,
    packageId: spec.packageId,
    providerId: spec.providerId,
    bridgeScope: spec.packageId,
    name: spec.name,
    version: spec.version,
    site: spec.site,
    language: spec.language,
    startUrl: spec.startUrl,
    entry: MAIN_FILE,
    requiresVerification: Boolean(spec.requiresVerification),
    description: spec.description || '',
  };

  const sourceDir = path.join(PACKAGE_SOURCE_ROOT, spec.packageId);
  const sourceScriptPath = path.join(sourceDir, MAIN_FILE);
  const generatedScript = renderScript(spec);
  const packageDir = path.join(DIST_PACKAGE_ROOT, spec.packageId);
  const bundledAssetDir = path.join(APP_ASSET_ROOT, spec.packageId);
  fs.mkdirSync(packageDir, { recursive: true });
  fs.mkdirSync(bundledAssetDir, { recursive: true });
  fs.writeFileSync(path.join(packageDir, MANIFEST_FILE), JSON.stringify(manifest, null, 2) + '\n');
  fs.writeFileSync(path.join(packageDir, MAIN_FILE), generatedScript);
  fs.writeFileSync(path.join(bundledAssetDir, MANIFEST_FILE), JSON.stringify(manifest, null, 2) + '\n');
  fs.writeFileSync(path.join(bundledAssetDir, MAIN_FILE), generatedScript);

  const zip = new JSZip();
  zip.file(MANIFEST_FILE, JSON.stringify(manifest, null, 2));
  zip.file(MAIN_FILE, generatedScript);

  if (fs.existsSync(sourceDir)) {
    const extraFiles = fs.readdirSync(sourceDir).filter(name => ![MAIN_FILE, MANIFEST_FILE].includes(name));
    for (const extraFile of extraFiles) {
      const extraPath = path.join(sourceDir, extraFile);
      if (fs.statSync(extraPath).isFile()) {
        const targetPath = path.join(packageDir, extraFile);
        const bundledTargetPath = path.join(bundledAssetDir, extraFile);
        fs.copyFileSync(extraPath, targetPath);
        fs.copyFileSync(extraPath, bundledTargetPath);
        zip.file(extraFile, fs.readFileSync(extraPath));
      }
    }
  }

  const artifactName = `${spec.packageId}-${spec.version}${PACKAGE_EXTENSION}`;
  const outputPath = path.join(DIST_ROOT, artifactName);
  const archive = await zip.generateAsync({
    type: 'nodebuffer',
    compression: 'DEFLATE',
    compressionOptions: { level: 9 },
  });
  fs.writeFileSync(outputPath, archive);

  feed.push({
    packageId: spec.packageId,
    providerId: spec.providerId,
    name: spec.name,
    version: spec.version,
    site: spec.site,
    language: spec.language,
    requiresVerification: Boolean(spec.requiresVerification),
    description: spec.description || '',
    artifact: artifactName,
    sizeBytes: archive.length,
    sha256: crypto.createHash('sha256').update(archive).digest('hex'),
  });

  if (fs.existsSync(sourceScriptPath)) {
    console.log(`Built ${artifactName} (explicit runtime source)`);
  } else {
    console.log(`Built ${artifactName} (template runtime: ${spec.mode})`);
  }
}

fs.writeFileSync(
  path.join(DIST_ROOT, 'miyu-source-packages.json'),
  JSON.stringify(feed, null, 2) + '\n',
);

console.log(`Wrote ${feed.length} source package(s) to ${DIST_ROOT}`);
console.log(`Synced bundled source package assets to ${APP_ASSET_ROOT}`);

function renderScript(spec) {
  const explicitScriptPath = path.join(PACKAGE_SOURCE_ROOT, spec.packageId, MAIN_FILE);
  if (fs.existsSync(explicitScriptPath)) {
    // Some providers, such as WTR-LAB, need a dedicated runtime instead of the shared template.
    return fs.readFileSync(explicitScriptPath, 'utf8');
  }
  if (!template) {
    throw new Error(`No template runtime found for ${spec.packageId}`);
  }
  const runtimeConfig = {
    mode: spec.mode,
    bridgeScope: spec.packageId,
    providerLabel: spec.name,
    providerId: spec.providerId,
    baseUrl: spec.config.baseUrl,
    startUrl: spec.startUrl,
  };
  return template.replace('__MIYU_CONFIG__', JSON.stringify(runtimeConfig, null, 2));
}

function validateSpec(spec, packageIds, providerIds) {
  const requiredFields = [
    'packageId',
    'providerId',
    'mode',
    'name',
    'version',
    'site',
    'language',
    'startUrl',
  ];
  for (const field of requiredFields) {
    if (!spec[field] || String(spec[field]).trim() === '') {
      throw new Error(`Package is missing required field: ${field}`);
    }
  }
  if (!spec.config || !spec.config.baseUrl) {
    throw new Error(`Package ${spec.packageId} is missing config.baseUrl`);
  }
  if (!/^[a-z0-9._-]+$/.test(spec.packageId)) {
    throw new Error(`Invalid package id: ${spec.packageId}`);
  }
  if (packageIds.has(spec.packageId)) {
    throw new Error(`Duplicate package id: ${spec.packageId}`);
  }
  if (providerIds.has(spec.providerId)) {
    throw new Error(`Duplicate provider id in source package catalog: ${spec.providerId}`);
  }
  packageIds.add(spec.packageId);
  providerIds.add(spec.providerId);
}
