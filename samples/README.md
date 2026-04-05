# Sample submissions

These payloads demonstrate the different validation outcomes.

**Option 1 -- curl:**
```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d @samples/<filename>.json
```

**Option 2 -- Swagger UI:**

Open `http://localhost:8080/swagger-ui`, expand the `POST /products` endpoint, click "Try it out", and paste the contents of any sample file into the request body.

| File | Expected outcome |
|------|-----------------|
| `valid-submission.json` | Product and track reach `VALIDATED` |
| `needs-review-past-release-date.json` | Product reaches `NEEDS_REVIEW` due to past release date. Fix the release date via `PUT /products/{id}` and the pipeline will revalidate automatically. |
| `validation-failed-invalid-audio-format.json` | Track reaches `VALIDATION_FAILED` due to unsupported audio format. Fix the audio file URI via `PUT /products/{id}` and the pipeline will revalidate automatically. |
| `validation-failed-missing-main-artist.json` | Track reaches `VALIDATION_FAILED` due to missing MAIN_ARTIST contributor. Fix the contributors via `PUT /products/{id}` and the pipeline will revalidate automatically. |

After submitting, use `GET /products/{id}` with the returned ID to check the validation status. Allow a few seconds for the pipeline to process.