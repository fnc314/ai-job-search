# Backend Subsystem: Cloud Run Services & AI Processing Pipeline

The `backend` subsystem provides cloud-native automation, real-time message ingestion, candidate-job matching algorithms, and automated pipeline enrichment.

## Modules

- **`:backend:service`**: Main Ktor HTTP service orchestrating cloud workflows and webhooks on Google Cloud Run.
- **`:backend:job-post-ranking`**: Unified deterministic candidate-to-job fit scoring engine.
- **`:backend:tracker-conventions`**: Shared backend conventions and utility wrappers.
- **`:backend:email-classification`**: Ingestion classification logic for inbound status communications.
- **`:backend:company-metadata-enrichment`**: Automatic metadata extraction and profile enrichment.
- **`:backend:event-chain`**: Event sequencing and state progression verification.
