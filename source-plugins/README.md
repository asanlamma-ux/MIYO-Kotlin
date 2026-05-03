## Source Plugins

This workspace builds the external source packages that MIYO can import from release assets.

Layout:
- `catalog.json`: enabled providers and package metadata
- `templates/source-package.js`: shared runtime for HTML-based providers
- `packages/<package-id>/main.js`: explicit runtime overrides for providers that need custom logic

Build locally:

```bash
npm ci
npm run build:source-packages
```

Artifacts are written to `source-plugins/.dist/`:
- `*.miyuplugin.zip`
- `miyu-source-packages.json`

The package catalog is intentionally curated. Broken or discontinued providers should be removed here instead of being carried forward as dead entries.
