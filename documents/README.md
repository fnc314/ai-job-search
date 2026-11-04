# Documents Folder

This folder is largely deprecated. Most subdirectories have been relocated:

- **CV files** → `templates/cv/`
- **Cover letter templates** → `templates/cover-letters/`
- **Job applications** → `job-applications/` (top-level)
- **LinkedIn, diplomas, references, postings** → removed (contained only `.gitkeep` placeholders)

This README is kept for reference only. The actual document locations are:

```
templates/
├── cv/                          # CV templates and tailored CVs
│   ├── main_example.tex         # Master CV template
│   └── main_<company>_<role>.tex
├── cover-letters/               # Cover letter templates
│   ├── cover_example.tex
│   └── cover_<company>_<role>.tex
job-applications/                # Past job applications (lower-kebab-case)
└── <company>/
    └── <role>/
        └── <YYYY-MM-DD>/
            ├── job_posting.md
            ├── main_<company>_<role>.tex
            ├── cover_<company>_<role>.tex
            └── outcome.md
```

See `templates/README.md` for template details and `job-applications/` for application records.
