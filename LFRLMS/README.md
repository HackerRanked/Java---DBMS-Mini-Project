# Luxury Fleet Rental & Leasing Management System

## Database setup

Install MySQL Server 8.0.16 or newer. The schema uses enforced `CHECK` constraints.

From Command Prompt, change to the `LFRLMS` project folder and run:

```bat
setup_database.bat
```

The script uses the MySQL client at `C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe`, or falls back to `mysql` on `PATH`. It opens one client session, so enter your MySQL password once when prompted. The password is not stored in the script.

The scripts run in this order: schema, functions and procedures, views, triggers, seed data, optional `06_demo_queries.sql` when that later-phase file exists, and negative tests. The seed ends with the F12 consistency query; it should display column headers and zero data rows. The final section prints expected constraint errors from the negative tests; those errors mean the invalid statements were rejected. Review the full output for any errors before the negative-test section.

The setup command uses `--force` so the client can continue through the intentionally invalid statements in `07_negative_tests.sql`. Run it once against a fresh database.

If `lfrlms` already contains an earlier schema, run `setup_database.bat /reset`. It asks you to type `RESET`, then drops and rebuilds the whole project database in one MySQL session. This permanently deletes all existing data in `lfrlms`; use it only when that data can be discarded. Paste the complete output if anything fails.

## Local JDBC configuration

Copy `config/db.properties.example` to `config/db.properties` and set the username and password for your local MySQL account. The real credentials file is ignored by Git.
