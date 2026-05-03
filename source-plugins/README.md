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

Each package manifest declares the JavaScript bridge capabilities it supports:
`search`, `details`, `chapter`, and `chapters`. It also declares the HTTPS
hosts the runtime is allowed to load. The Android importer validates both before
installing a package, and the hidden WebView runtime blocks navigation outside
the package allow-list.

The package catalog is intentionally curated. Broken or discontinued providers
should be removed here instead of being carried forward as dead entries.
