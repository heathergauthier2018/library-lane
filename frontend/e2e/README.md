# Library Lane automation

Run deterministic browser, reliability, and accessibility checks:

```powershell
npm run test:e2e
```

Open Playwright's interactive runner:

```powershell
npm run test:e2e:ui
```

Run the real external catalog separately:

```powershell
$env:LIBRARY_LANE_API_URL="http://localhost:8080"
npm run test:live-catalog
```

The normal suite mocks catalog providers so Apple, Google, Spotify, Open
Library, or Wikidata availability cannot make local regression checks flaky.
